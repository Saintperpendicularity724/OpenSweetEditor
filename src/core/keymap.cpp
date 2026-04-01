#include <keymap.h>
#include <utility.h>

namespace NS_SWEETEDITOR {

  bool KeyChord::operator==(const KeyChord& other) const {
    return key_code == other.key_code && modifiers == other.modifiers;
  }

  bool KeyChord::operator!=(const KeyChord& other) const {
    return !(*this == other);
  }

  void KeyMap::addBinding(const KeyBinding& binding) {
    if (binding.first.empty()) return;
    if (binding.second.empty()) {
      m_entries_[binding.first] = binding.command;
    } else {
      auto it = m_entries_.find(binding.first);
      if (it == m_entries_.end()) {
        HashMap<KeyChord, EditorCommand, KeyChordHash> sub;
        sub[binding.second] = binding.command;
        m_entries_[binding.first] = std::move(sub);
      } else if (auto* sub = std::get_if<HashMap<KeyChord, EditorCommand, KeyChordHash>>(&it->second)) {
        (*sub)[binding.second] = binding.command;
      } else {
        // Overwrite a single-chord entry with a sub-map
        HashMap<KeyChord, EditorCommand, KeyChordHash> sub_map;
        sub_map[binding.second] = binding.command;
        it->second = std::move(sub_map);
      }
    }
  }

  const KeyMapEntry* KeyMap::lookup(const KeyChord& chord) const {
    auto it = m_entries_.find(chord);
    if (it == m_entries_.end()) return nullptr;
    return &it->second;
  }

  static void addCmd(KeyMap& km, KeyModifier mods, KeyCode key, EditorCommand cmd) {
    km.addBinding({{mods, key}, {}, cmd});
  }

  KeyMap KeyMap::createDefault() {
    KeyMap km;
    using KC = KeyCode;
    using KM = KeyModifier;
    using EC = EditorCommand;

    // Cursor movement
    addCmd(km, KM::NONE,  KC::LEFT,  EC::CURSOR_LEFT);
    addCmd(km, KM::NONE,  KC::RIGHT, EC::CURSOR_RIGHT);
    addCmd(km, KM::NONE,  KC::UP,    EC::CURSOR_UP);
    addCmd(km, KM::NONE,  KC::DOWN,  EC::CURSOR_DOWN);
    addCmd(km, KM::NONE,  KC::HOME,  EC::CURSOR_LINE_START);
    addCmd(km, KM::NONE,  KC::END,   EC::CURSOR_LINE_END);
    addCmd(km, KM::NONE, KC::PAGE_UP,   EC::CURSOR_PAGE_UP);
    addCmd(km, KM::NONE, KC::PAGE_DOWN, EC::CURSOR_PAGE_DOWN);

    // Selection (Shift + movement)
    addCmd(km, KM::SHIFT, KC::LEFT,  EC::SELECT_LEFT);
    addCmd(km, KM::SHIFT, KC::RIGHT, EC::SELECT_RIGHT);
    addCmd(km, KM::SHIFT, KC::UP,    EC::SELECT_UP);
    addCmd(km, KM::SHIFT, KC::DOWN,  EC::SELECT_DOWN);
    addCmd(km, KM::SHIFT, KC::HOME,  EC::SELECT_LINE_START);
    addCmd(km, KM::SHIFT, KC::END,   EC::SELECT_LINE_END);
    addCmd(km, KM::SHIFT, KC::PAGE_UP,   EC::SELECT_PAGE_UP);
    addCmd(km, KM::SHIFT, KC::PAGE_DOWN, EC::SELECT_PAGE_DOWN);

    // Editing
    addCmd(km, KM::NONE, KC::BACKSPACE,  EC::BACKSPACE);
    addCmd(km, KM::NONE, KC::DELETE_KEY, EC::DELETE_FORWARD);
    addCmd(km, KM::NONE,  KC::TAB,   EC::INSERT_TAB);
    addCmd(km, KM::NONE,  KC::ENTER, EC::INSERT_NEWLINE);

    // Ctrl/Cmd shortcuts
    addCmd(km, KM::CTRL, KC::A, EC::SELECT_ALL);
    addCmd(km, KM::META, KC::A, EC::SELECT_ALL);
    addCmd(km, KM::CTRL, KC::Z, EC::UNDO);
    addCmd(km, KM::META, KC::Z, EC::UNDO);
    addCmd(km, KM::CTRL | KM::SHIFT, KC::Z, EC::REDO);
    addCmd(km, KM::META | KM::SHIFT, KC::Z, EC::REDO);
    addCmd(km, KM::CTRL, KC::Y, EC::REDO);
    addCmd(km, KM::META, KC::Y, EC::REDO);

    // Clipboard (platform-handled)
    addCmd(km, KM::CTRL, KC::C, EC::COPY);
    addCmd(km, KM::META, KC::C, EC::COPY);
    addCmd(km, KM::CTRL, KC::V, EC::PASTE);
    addCmd(km, KM::META, KC::V, EC::PASTE);
    addCmd(km, KM::CTRL, KC::X, EC::CUT);
    addCmd(km, KM::META, KC::X, EC::CUT);

    // Line operations (Ctrl/Cmd + Enter)
    addCmd(km, KM::CTRL, KC::ENTER, EC::INSERT_LINE_BELOW);
    addCmd(km, KM::META, KC::ENTER, EC::INSERT_LINE_BELOW);
    addCmd(km, KM::CTRL | KM::SHIFT, KC::ENTER, EC::INSERT_LINE_ABOVE);
    addCmd(km, KM::META | KM::SHIFT, KC::ENTER, EC::INSERT_LINE_ABOVE);

    // Line operations (Alt + arrow)
    addCmd(km, KM::ALT, KC::UP,   EC::MOVE_LINE_UP);
    addCmd(km, KM::ALT, KC::DOWN, EC::MOVE_LINE_DOWN);
    addCmd(km, KM::ALT | KM::SHIFT, KC::UP,   EC::COPY_LINE_UP);
    addCmd(km, KM::ALT | KM::SHIFT, KC::DOWN, EC::COPY_LINE_DOWN);

    // Delete line (Ctrl/Cmd + Shift + K)
    addCmd(km, KM::CTRL | KM::SHIFT, KC::K, EC::DELETE_LINE);
    addCmd(km, KM::META | KM::SHIFT, KC::K, EC::DELETE_LINE);

    return km;
  }

  KeyResolver::KeyResolver(int64_t pending_timeout_ms)
    : m_pending_timeout_ms_(pending_timeout_ms) {}

  void KeyResolver::setKeyMap(KeyMap key_map) {
    m_key_map_ = std::move(key_map);
    cancelPending();
  }

  ResolveResult KeyResolver::resolve(const KeyChord& chord) {
    if (m_pending_) {
      bool expired = !m_pending_sub_map_ ||
                     (TimeUtil::milliTime() - m_pending_time_ > m_pending_timeout_ms_);
      if (expired) {
        cancelPending();
      } else {
        auto it = m_pending_sub_map_->find(chord);
        cancelPending();
        if (it != m_pending_sub_map_->end()) {
          return {ResolveStatus::MATCHED, it->second};
        }
        return {ResolveStatus::NO_MATCH, EditorCommand::NONE};
      }
    }

    const KeyMapEntry* entry = m_key_map_.lookup(chord);
    if (!entry) return {ResolveStatus::NO_MATCH, EditorCommand::NONE};

    if (auto* cmd = std::get_if<EditorCommand>(entry)) {
      return {ResolveStatus::MATCHED, *cmd};
    }
    if (auto* sub = std::get_if<HashMap<KeyChord, EditorCommand, KeyChordHash>>(entry)) {
      m_pending_ = true;
      m_pending_time_ = TimeUtil::milliTime();
      m_pending_sub_map_ = sub;
      return {ResolveStatus::PENDING, EditorCommand::NONE};
    }
    return {ResolveStatus::NO_MATCH, EditorCommand::NONE};
  }

  void KeyResolver::cancelPending() {
    m_pending_ = false;
    m_pending_time_ = 0;
    m_pending_sub_map_ = nullptr;
  }

} // namespace NS_SWEETEDITOR
