def create_drawing_note(title, shot=None):
    if not tap_desc("New Note"):
        return False
    if not tap_text("Drawing Note"):
        return False
    # The drawing canvas is ready only once the drawing top bar is on screen.
    # Canvas settings now live in the "Drawing options" overflow menu, so the
    # readiness probe looks for the always-present bar controls instead.
    if not wait_find(lambda x: find(x, desc="Drawing options") or
                     (find(x, desc="Back") and find(x, text="Title")), timeout=20):
        log("   create_drawing_note: drawing canvas did not open")
        return False
    w, h = screen_size()
    draw_star(w * 0.5, h * 0.46, h * 0.18)
    type_into("Title", title)
    type_into_field(0, title)
