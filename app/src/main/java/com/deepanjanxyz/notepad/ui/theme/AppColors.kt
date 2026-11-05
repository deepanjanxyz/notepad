package com.deepanjanxyz.notepad.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The fixed colours the drawing, editor and note surfaces are built from.
 *
 * These screens sit on their own dark chrome rather than on the app's Material
 * palette, so their colours are kept here as named values instead of being
 * written out as literals at each use. Every value is exactly the one the
 * screens used before, so nothing on screen changes.
 */
object AppColors {

    // --- Accents -----------------------------------------------------------------

    /** Accent for a selected tool, the brush and the active controls. */
    val Accent = Color(0xFF2196F3)

    /** A faint accent wash used behind a marquee selection. */
    val AccentWash = Color(0x222196F3)

    /** Accent for the checklist and label highlights. */
    val AccentAmber = Color(0xFFFFB300)

    /** Accent for the checklist card on the create-note sheet. */
    val AccentGreen = Color(0xFF4CAF50)

    /** Outline of a selected accent control. */
    val AccentEdge = Color(0xFF64B5F6)

    /** Destructive action colour. */
    val Danger = Color(0xFFEF5350)

    /** Dim wash drawn over a drawing preview. */
    val Scrim = Color(0x99000000)

    // --- Panels and surfaces -----------------------------------------------------

    /** Background of the panel that holds the drawing tools. */
    val Panel = Color(0xFF1A1A1E)

    /** Background of the drawing screen and its bottom toolbar. */
    val PanelDeep = Color(0xFF131314)

    /** Background of the canvas background dialog. */
    val DialogSurface = Color(0xFF202124)

    /** Background of the editor's colour picker sheet. */
    val SheetSurface = Color(0xFF1E1E22)

    /** Background of the drawing colour palette sheet. */
    val SheetSurfaceAlt = Color(0xFF1E1F22)

    /** Background of an outlined input field on a dark sheet. */
    val FieldSurface = Color(0xFF2B2C30)

    /** Background of an unselected chip on a dark screen. */
    val ChipSurface = Color(0xFF2C2C32)

    /** Background of the drawing selection overlay. */
    val SelectionSurface = Color(0xFF26272E)

    /** Background behind an amber-accented control. */
    val AmberSurface = Color(0xFF282419)

    /** Background of the destructive "clear" control. */
    val DangerSurface = Color(0xFF352024)

    // --- Controls and outlines ---------------------------------------------------

    /** Fill of a control that is not selected. */
    val ControlIdle = Color(0xFF2A2A30)

    /** Outline of a control that is not selected. */
    val ControlEdge = Color(0xFF404048)

    /** Outline of an unselected chip or swatch. */
    val FieldOutline = Color(0xFF42434D)

    /** Outline of a label input field. */
    val InputOutline = Color(0xFF48484F)

    /** Track of a slider that is not being dragged. */
    val TrackIdle = Color(0xFF424248)

    /** Outline of the drawing selection overlay. */
    val SelectionEdge = Color(0xFF3E404C)

    /** A thin divider on a dark sheet. */
    val DividerLine = Color(0xFF2C2D31)

    /** Fill of a size dot that is not selected. */
    val DotIdle = Color(0xFF2E2E36)

    /** Fill of a thickness dot that is not selected. */
    val DotIdleLight = Color(0xFFB0B0B0)

    /** Outline of the white swatch when it is not selected. */
    val SwatchIdle = Color(0xFF666666)

    // --- Text and icons ----------------------------------------------------------

    /** Primary text on a dark editor surface. */
    val TextPrimary = Color(0xFFE2E2E6)

    /** Text on a dark control that is not selected. */
    val TextIdle = Color(0xFFCCCCCC)

    /** Secondary label text on the drawing panel. */
    val TextLabel = Color(0xFF9093A0)

    /** Muted icon and helper text. */
    val TextMuted = Color(0xFF8E9099)

    /** Dim text on a sheet. */
    val TextDim = Color(0xFF757579)

    /** Placeholder text in an empty field. */
    val TextPlaceholder = Color(0xFF6E7179)

    /** Faint text on a sheet. */
    val TextFaint = Color(0xFF9E9E9E)

    /** Hint text inside a dialog. */
    val TextHint = Color(0xFFB0B3BD)

    /** Text drawn on top of the amber accent. */
    val TextOnAmber = Color(0xFF1B1B1F)

    /** The opaque white a note colour is compared against when it is the default. */
    const val OPAQUE_WHITE = 0xFFFFFFFFL

    // --- Toolbar and editor extras -----------------------------------------------

    /** Cap of the eraser icon on the dark bottom toolbar. */
    val IconIdleOnTool = Color(0xFFC0C2CC)

    /** Background of a label pill in the editor. */
    val LabelPillContainer = Color(0xFF382952)

    /** Text of a label pill in the editor. */
    val LabelPillContent = Color(0xFFD0BCFF)
}
