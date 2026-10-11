# Eyeliner

Eyeliner is a [Minestom](https://minestom.net) library for building in-world user interfaces from HTML-like markup.
Menus are written in `.eyeliner` files, rendered with display entities and shown to players through packets only.
Players interact with them by looking at a button and clicking.

```html
<screen width="4" height="2" bg="#14141ee6" variant="rounded">
    <text size="5" color="#f8a407">Main Menu</text>

    <button onClick="joinGame" variant="pill primary">
        <sprite item="minecraft:diamond" mr="0.05" />
        <text>Join Game</text>
    </button>
</screen>
```

```java
Eyeliner.registerAction("joinGame", ctx -> {
    ctx.player().sendMessage(Component.text("Joining game..."));
    ctx.close();
});

Menu menu = Eyeliner.loadResource("main.eyeliner");
menu.open(player);
```

A complete showcase of every feature is in [`src/main/resources/example.eyeliner`](src/main/resources/example.eyeliner).

---

## Contents

- [How it works](#how-it-works)
- [The .eyeliner format](#the-eyeliner-format)
- [Values: units, colors, booleans](#values-units-colors-booleans)
- [Elements](#elements)
  - [`<screen>`](#screen)
  - [`<text>`](#text)
  - [`<button>`](#button)
  - [`<sprite>`](#sprite)
- [Common attributes](#common-attributes)
- [Layout](#layout)
- [Backgrounds, borders and rounded corners](#backgrounds-borders-and-rounded-corners)
- [Transparency](#transparency)
- [Hover effects](#hover-effects)
- [Click sounds](#click-sounds)
- [Variants](#variants)
- [Inheritance and default values](#inheritance-and-default-values)
- [Attribute aliases](#attribute-aliases)
- [Java API](#java-api)
  - [Loading menus](#loading-menus)
  - [Actions](#actions)
  - [Opening menus](#opening-menus)
  - [Placeholders](#placeholders)
  - [Menu sessions](#menu-sessions)
  - [Custom variants](#custom-variants)
- [Player interaction](#player-interaction)
- [Errors and warnings](#errors-and-warnings)
- [Limitations](#limitations)

---

## How it works

- **Packet-only.** A menu exists only on the client of the player it was opened for. It is never added to an
  instance, never ticks and is invisible to everyone else. Many players can view the same menu at the same time,
  each with their own independent copy.
- **Display entities.** Backgrounds and borders are text displays whose background is stretched into rectangles,
  text is rendered with text displays and sprites with item displays.
- **Interaction.** When a player clicks, Eyeliner traces their line of sight onto the screen to find the button
  under the crosshair. Invisible interaction entities sit in front of every button so right clicks reach the server
  and clicking a button never breaks the block behind it.
- **Parse once, open many times.** A `.eyeliner` file is parsed into an immutable `Menu`. Each `open(...)` lays it
  out and renders it for one player.

---

## The .eyeliner format

The syntax is close to HTML/XML:

```html
<!-- Comments are allowed anywhere -->
<screen width="4" height="2">
    <text size="3">Hello</text>
    <button onClick="play">Play</button>
    <sprite item="minecraft:diamond" />
</screen>
```

- A document has exactly **one root element, which must be `<screen>`**.
- Tag and attribute names are **case-insensitive** (`onClick`, `onclick` and `ONCLICK` are the same).
- Attribute values can use double quotes, single quotes or no quotes: `size="2"`, `size='2'`, `size=2`.
- An attribute without a value is `true`: `<sprite item="minecraft:apple" glint />`.
- Elements without children can be self-closing: `<sprite item="minecraft:apple" />`.
- Whitespace in text collapses to single spaces (like HTML), so text can be split over multiple lines.
- Outside of `<text>`, the entities `&lt;` `&gt;` `&amp;` `&quot;` `&apos;` are decoded.
- Inside `<text>`, the content is taken **as-is** and parsed with [MiniMessage](https://docs.advntr.dev/minimessage/format.html),
  so tags like `<bold>`, `<gradient:red:blue>` or `<newline>` work. To show a literal `<`, escape it as `\<`.
- Text placed directly inside a `<button>` or `<screen>` becomes a `<text>` element automatically:
  `<button onClick="play">Play</button>` is the same as `<button onClick="play"><text>Play</text></button>`.

### Allowed nesting

| Element    | May contain                    |
|------------|--------------------------------|
| `<screen>` | `<text>`, `<button>`, `<sprite>` |
| `<button>` | `<text>`, `<sprite>`           |
| `<text>`   | MiniMessage text only          |
| `<sprite>` | nothing                        |

---

## Values: units, colors, booleans

### Lengths

All sizes are in **blocks**.

| Syntax  | Meaning                                                               |
|---------|-----------------------------------------------------------------------|
| `1.5`   | 1.5 blocks                                                            |
| `50%`   | 50% of the parent's content box (width for `width`, height for `height`) |
| `auto`  | size to the content (only for `width` / `height`)                     |

### Colors

| Syntax      | Example       | Meaning                                    |
|-------------|---------------|--------------------------------------------|
| `#RGB`      | `#f80`        | short hex, opaque                          |
| `#RGBA`     | `#f808`       | short hex with alpha                       |
| `#RRGGBB`   | `#ff8800`     | hex, opaque                                |
| `#RRGGBBAA` | `#ff880080`   | hex with alpha **last** (like CSS)         |
| name        | `gold`        | a named color, see below                   |

Named colors: `transparent` (also `none`), `black`, `white`, `red`, `dark_red`, `green`, `dark_green`, `blue`,
`dark_blue`, `yellow`, `gold`, `aqua`, `dark_aqua`, `light_purple`, `dark_purple`, `gray`/`grey`,
`dark_gray`/`dark_grey`. They use the Minecraft chat color values.

### Booleans

`true`, `yes`, `on`, `1` and `false`, `no`, `off`, `0`. Writing the attribute without a value means `true`.

---

## Elements

### `<screen>`

The root panel. It decides the size of the menu and where it appears.

```html
<screen width="4" height="2.5" distance="2.5" closeDistance="8" bg="#101018cc" variant="rounded">
    ...
</screen>
```

| Attribute       | Default     | Description                                                                                     |
|-----------------|-------------|-------------------------------------------------------------------------------------------------|
| `width`         | `3`         | Width in blocks                                                                                 |
| `height`        | `2`         | Height in blocks                                                                                |
| `padding`       | `0.15`      | Space between the edge and the content                                                          |
| `gap`           | `0.1`       | Space between children                                                                          |
| `direction`     | `column`    | Children are stacked vertically                                                                 |
| `valign`        | `top`       | Content starts at the top                                                                       |
| `bg`            | `#14141ee6` | Dark, slightly see-through background                                                           |
| `distance`      | `2.5`       | When opened in front of a player: distance in blocks from their eyes to the screen               |
| `reach`         | `6`         | Maximum distance in blocks at which buttons can be hovered and clicked                          |
| `closeDistance` | `0`         | Closes the menu automatically once the player is further away than this. `0` disables it        |
| `brightness`    | `15`        | Light level (0–15) the UI is rendered with. `15` = always fully lit, `-1` = use the world's light |

All [common attributes](#common-attributes) work as well.

### `<text>`

A text label. The content is MiniMessage.

```html
<text size="4" color="gold" shadow="true">Welcome, <bold><player></bold>!</text>
<text width="90%" textAlign="left" size="1.5">A longer text that wraps automatically when it does not fit.</text>
```

| Attribute   | Default   | Inherited | Description                                                                                         |
|-------------|-----------|-----------|-----------------------------------------------------------------------------------------------------|
| `color`     | `#ffffff` | yes       | Base text color. MiniMessage colors inside the text override it. The alpha channel sets the text opacity |
| `size`      | `2`       | yes       | Font size. `1` = 1/20 block of line height, so `size="5"` is the size of a vanilla text display     |
| `shadow`    | `false`   | yes       | Draws a drop shadow behind the text                                                                 |
| `textAlign` | `center`  | yes       | `left`, `center` or `right`. Aligns the lines and positions the text inside its box                 |

**Wrapping:** text wraps automatically when it is wider than the space available. Set `width` to control the wrap
width. Lines break at spaces like in vanilla Minecraft; use `<newline>` (or `<br>`) to force a line break.

A `<text>` can also have a background, border, padding and so on (see [common attributes](#common-attributes)).

### `<button>`

A clickable container. It lays out its children (usually a sprite and a text) in a row.

```html
<button onClick="joinGame" variant="rounded" bg="#22c55e80" hoverBg="#22c55ee0"
        sound="minecraft:entity.experience_orb.pickup" soundVolume="0.8" soundPitch="1.4">
    <sprite item="minecraft:diamond_sword" size="0.22" mr="0.05" glint />
    <text>Join Game</text>
</button>
```

| Attribute      | Default                     | Description                                                                            |
|----------------|-----------------------------|----------------------------------------------------------------------------------------|
| `onClick`      | –                           | Action key that runs on left **and** right click                                       |
| `onLeftClick`  | –                           | Action key for left clicks. Takes priority over `onClick`                              |
| `onRightClick` | –                           | Action key for right clicks. Takes priority over `onClick`                             |
| `hoverBg`      | lighter version of `bg`     | Background while the player looks at the button, see [Hover effects](#hover-effects)   |
| `sound`        | `minecraft:ui.button.click` | Sound played when clicked. `none` disables it, see [Click sounds](#click-sounds)        |
| `soundVolume`  | `0.6`                       | Volume of the click sound. `0` disables it                                             |
| `soundPitch`   | `1`                         | Pitch of the click sound, `0.5` to `2`                                                 |
| `direction`    | `row`                       | Children are placed next to each other                                                 |
| `padding`      | `0.06 0.12`                 | 0.06 blocks top/bottom, 0.12 blocks left/right                                         |
| `gap`          | `0.05`                      | Space between children                                                                 |
| `bg`           | `#3a3a4c`                   | Background color                                                                       |

Action keys are plain names that you bind to Java code, see [Actions](#actions). A button without any action
still shows the hover effect but does nothing when clicked and plays no sound.

### `<sprite>`

An item, rendered like in an inventory slot. Block items appear as small 3D blocks.

```html
<sprite item="minecraft:diamond" size="0.3" />
<sprite item="golden_apple" glint />
```

| Attribute | Default             | Description                                                                         |
|-----------|---------------------|-------------------------------------------------------------------------------------|
| `item`    | `minecraft:barrier` | Item id. The `minecraft:` prefix is optional. Unknown items fall back to a barrier   |
| `size`    | `0.25`              | Edge length in blocks. On a sprite, `size` means this, not the font size             |
| `glint`   | `false`             | Adds the enchantment glint                                                          |

---

## Common attributes

These work on every element.

### General

| Attribute | Description                                                                                       |
|-----------|---------------------------------------------------------------------------------------------------|
| `id`      | A name for the element. Used to change text at runtime with `MenuSession.setText(id, ...)`         |
| `variant` | One or more variant names separated by spaces, applied in order: `variant="pill primary"`          |

### Size and spacing

| Attribute                     | Default | Description                                                         |
|-------------------------------|---------|---------------------------------------------------------------------|
| `width`, `height`             | `auto`  | Fixed size in blocks, a percentage of the parent, or `auto`          |
| `padding`                     | `0`     | Inner spacing, 1 to 4 values like CSS (see below)                    |
| `pt`, `pr`, `pb`, `pl`        | `0`     | Padding top / right / bottom / left                                 |
| `px`, `py`                    |         | Padding left+right / top+bottom                                     |
| `margin`                      | `0`     | Outer spacing, 1 to 4 values like CSS                                |
| `mt`, `mr`, `mb`, `ml`        | `0`     | Margin top / right / bottom / left (can be negative)                |
| `mx`, `my`                    |         | Margin left+right / top+bottom                                      |

`padding` and `margin` take 1 to 4 values:

| Value              | Meaning                                    |
|--------------------|--------------------------------------------|
| `0.1`              | all sides                                  |
| `0.1 0.2`          | top+bottom, left+right                     |
| `0.1 0.2 0.3`      | top, left+right, bottom                    |
| `0.1 0.2 0.3 0.4`  | top, right, bottom, left (clockwise)       |

The more specific attribute wins regardless of order: `padding="0.1" pl="0.3"` gives a left padding of 0.3.

### Layout of children

| Attribute   | Default             | Values                                       | Description                                  |
|-------------|---------------------|----------------------------------------------|----------------------------------------------|
| `direction` | `column`            | `column`, `row`                              | Stack children vertically or horizontally     |
| `align`     | value of `textAlign`| `left`, `center`, `right`                    | Horizontal alignment of the children          |
| `valign`    | `center`            | `top`, `center`, `bottom`                    | Vertical alignment of the children            |
| `gap`       | `0`                 | length                                       | Space between children                        |

`start`/`end` are accepted as synonyms for `left`/`right` and `top`/`bottom`, and `middle` for `center`.

### Appearance

| Attribute     | Default       | Description                                                                         |
|---------------|---------------|-------------------------------------------------------------------------------------|
| `bg`          | `transparent` | Background color                                                                    |
| `hoverBg`     | `auto`        | Background color while hovered (buttons only)                                       |
| `border`      | `0`           | Border thickness in blocks                                                          |
| `borderColor` | `#ffffff`     | Border color                                                                        |
| `radius`      | `0`           | Corner radius in blocks, or a percentage (`100%` = fully round ends)               |
| `color`       | `#ffffff`     | Text color (inherited)                                                              |
| `size`        | `2`           | Font size (inherited)                                                               |
| `shadow`      | `false`       | Text shadow (inherited)                                                             |
| `textAlign`   | `center`      | Text alignment (inherited)                                                          |

---

## Layout

Eyeliner uses a simplified flexbox model.

- **Containers** (`<screen>`, `<button>`) place their children one after another, in a column or a row
  (`direction`), with `gap` between them.
- **`align`** positions children horizontally and **`valign`** vertically, in both directions.
  For example, a column with `align="center"` centers every child horizontally; a row with `align="right"`
  pushes the whole row to the right.
- **Size:** an element is as large as its content plus padding, unless `width` / `height` is set.
  Percentages refer to the parent's content box (its size minus padding), so `width="100%"` fills the parent.
- **Margins** add space around an element without affecting its background.
- **Text in rows:** sprites and fixed-size elements are measured first; text gets the remaining width and
  wraps if needed.
- **`textAlign` on containers:** if `align` is not set, a container uses its `textAlign`, so
  `<button textAlign="center">` centers its content. Because `textAlign` is inherited, the button's text
  children also get it.
- Content that does not fit is not clipped. It overflows the screen.

Nested elements are drawn slightly in front of their parent, so children always render on top of backgrounds.

---

## Backgrounds, borders and rounded corners

Any element can have a background, a border and rounded corners:

```html
<button bg="#2f6fdf" border="0.015" borderColor="#ffffff80" radius="0.06">...</button>
<screen bg="#101018" radius="20%">...</screen>
<text bg="#00000080" padding="0.03 0.06" radius="0.03">Label with a background</text>
```

- `radius` in blocks is the radius of every corner. A percentage is relative to half of the shorter side,
  so `radius="100%"` gives a pill shape and `radius="50%"` gives clearly rounded corners at any size.
  The radius is limited automatically so corners never overlap.
- `border` draws a frame of the given thickness inside the element's box. With rounded corners the
  inner edge is rounded too.
- Rounded corners are drawn as small steps (a "pixel-art" curve). Larger radii use more steps.

---

## Transparency

Every color accepts an alpha channel as the **last** two hex digits (`#RRGGBBAA`) or the last digit of the
short form (`#RGBA`):

| Alpha | Opacity                |
|-------|------------------------|
| `00`  | fully transparent      |
| `40`  | ~25%                   |
| `80`  | ~50%                   |
| `c0`  | ~75%                   |
| `ff`  | opaque (same as no alpha) |

```html
<!-- Invisible screen: only the buttons float in the world -->
<screen bg="#00000000">
    <!-- 50% transparent green button, 88% when hovered -->
    <button bg="#22c55e80" hoverBg="#22c55ee0" onClick="play">Play</button>
</screen>
```

- A fully transparent `bg` creates no background entities.
- Text color alpha works too: `color="#ffffff99"` renders the text at ~60% opacity.
- Borders and fills never overlap, so semi-transparent backgrounds with borders have no darker seams.
  A semi-transparent button on a semi-transparent screen does blend, though: the two layers add up.

---

## Hover effects

When a player looks at a button, its background changes to `hoverBg`:

```html
<button bg="#3a3a4c" hoverBg="#5a5a7c">...</button>
<button bg="#2f6fdf80" hoverBg="#2f6fdfff">...</button>       <!-- becomes opaque on hover -->
<button bg="transparent" hoverBg="#ffffff30">...</button>     <!-- only visible on hover -->
```

- If `hoverBg` is not set, Eyeliner generates one: the background becomes 20% lighter (or 15% darker for very
  bright colors). The alpha of `bg` is kept.
- `hoverBg` can have its own transparency, so a button can be invisible until hovered.
- Only the button's own background changes. Borders, text and sprites stay the same.
- Hover state is per player and per menu, and updates as the player moves their view.

---

## Click sounds

Every button plays a sound when it is clicked. You can change the sound, its volume and its pitch, or turn it off:

```html
<button onClick="buy" sound="minecraft:entity.experience_orb.pickup" soundVolume="0.8" soundPitch="1.4">Buy</button>
<button onClick="back" soundPitch="0.8">Back</button>    <!-- default click sound, lower pitch -->
<button onClick="silent" sound="none">Silent</button>
```

| Attribute     | Default                     | Description                                                         |
|---------------|-----------------------------|---------------------------------------------------------------------|
| `sound`       | `minecraft:ui.button.click` | Any sound id, including custom sounds from a resource pack. `none` disables it |
| `soundVolume` | `0.6`                       | Volume. `0` disables the sound                                       |
| `soundPitch`  | `1`                         | Pitch from `0.5` (low) to `2` (high)                                 |

The sound plays only for the clicking player, and only if the button has an action for that click. Sounds can also be part of
a [variant](#variants), so you can give a whole group of buttons the same sound.

---

## Variants

Variants are named presets of attributes. Apply one or more with `variant`; they are applied left to right, and
attributes written on the element always win:

```html
<button variant="pill primary">Play</button>
<button variant="rounded danger" bg="#aa0000">Delete</button>   <!-- bg overrides the variant -->
```

### Built-in variants

Usable on any element:

| Variant   | Effect                                                                  |
|-----------|-------------------------------------------------------------------------|
| `flat`    | No rounding, no border (`radius="0" border="0"`)                         |
| `rounded` | Rounded corners (`radius="35%"`)                                         |
| `pill`    | Fully round ends (`radius="100%"`)                                       |
| `outline` | Transparent fill with a border (`bg="transparent" border="0.015" hoverBg="#ffffff30"`) |
| `ghost`   | Transparent until hovered (`bg="transparent" hoverBg="#ffffff30"`)       |

Usable on buttons:

| Variant     | Background |
|-------------|------------|
| `primary`   | `#2f6fdf`  |
| `secondary` | `#4b5563`  |
| `success`   | `#1f9d4c`  |
| `danger`    | `#d13b3b`  |

You can add your own variants from Java, see [Custom variants](#custom-variants).

---

## Inheritance and default values

Every attribute has a default, so nothing ever has to be set. The value of an attribute is decided in this order,
where later steps win:

1. **Global defaults** (for example `color="#ffffff"`, `size="2"`)
2. **Inherited values** from the parent element: `color`, `size`, `shadow` and `textAlign`
3. **Element defaults** (for example a button's `bg="#3a3a4c"`, see the tables under [Elements](#elements))
4. **Variants**, in the order they are listed
5. **Attributes written on the element**

Inheritance means you can set text styles once on a container:

```html
<button color="#f8a407" size="2.5" onClick="shop">
    <sprite item="minecraft:emerald" />
    <text>Shop</text>          <!-- gold and size 2.5, inherited from the button -->
</button>
```

If an attribute has an invalid value, Eyeliner logs a warning and uses the default (see
[Errors and warnings](#errors-and-warnings)).

---

## Attribute aliases

Several attributes have alternative names, for people used to CSS:

| Alias                                             | Same as        |
|---------------------------------------------------|----------------|
| `background`, `bgcolor`, `background-color`       | `bg`           |
| `hover-bg`, `hoverbackground`                     | `hoverBg`      |
| `font-size`, `fontsize`                           | `size` (text)  |
| `text-align`                                      | `textAlign`    |
| `border-radius`                                   | `radius`       |
| `border-color`                                    | `borderColor`  |
| `border-width`                                    | `border`       |
| `flex-direction`                                  | `direction`    |
| `p`                                               | `padding`      |
| `m`                                               | `margin`       |
| `padding-top`, `padding-right`, ...               | `pt`, `pr`, ...|
| `margin-top`, `margin-right`, ...                 | `mt`, `mr`, ...|
| `on-click`, `action`                              | `onClick`      |
| `click-sound`, `clicksound`                       | `sound`        |
| `volume`, `sound-volume`                          | `soundVolume`  |
| `pitch`, `sound-pitch`                            | `soundPitch`   |

---

## Java API

The main entry point is the static class `net.kalbskinder.Eyeliner`. Call `Eyeliner.init()` once after
`MinecraftServer.init()` to register Eyeliner's event listeners (or `Eyeliner.init(eventNode)` to attach them to
your own event node). If you forget, it is called automatically the first time a menu is opened.

### Loading menus

```java
Menu fromClasspath = Eyeliner.loadResource("menus/main.eyeliner");   // from src/main/resources
Menu fromFile      = Eyeliner.load(Path.of("menus/shop.eyeliner"));   // from disk, throws IOException
Menu fromString    = Eyeliner.parse("<screen><text>Hi</text></screen>");
```

Load a menu once (for example at startup) and reuse it. A `Menu` is immutable and can be opened for any number
of players.

### Actions

Buttons refer to actions by name (`onClick="joinGame"`). Bind the name to code:

```java
// For every menu
Eyeliner.registerAction("joinGame", ctx -> {
    Player player = ctx.player();
    player.sendMessage(Component.text("Joining..."));
    ctx.close();                           // close the menu that was clicked
});

// Only for one menu; takes priority over global actions with the same name
Menu shop = Eyeliner.loadResource("shop.eyeliner")
        .onClick("buy", ctx -> buy(ctx.player()))
        .onClick("back", ctx -> ctx.close());

Eyeliner.unregisterAction("joinGame");
```

The `ClickContext` passed to a handler contains:

| Method      | Description                                                    |
|-------------|----------------------------------------------------------------|
| `player()`  | The player who clicked                                         |
| `session()` | The open menu the button belongs to (see [Menu sessions](#menu-sessions)) |
| `button()`  | The clicked `ButtonElement` (its `id()`, `style()`, ...)        |
| `action()`  | The action name that was resolved                              |
| `type()`    | `ClickType.LEFT` or `ClickType.RIGHT`                          |
| `close()`   | Shortcut for `session().close()`                               |

Handlers run on the player's tick thread. If no handler is registered for an action, a warning is logged once and
the click is ignored. Exceptions thrown by a handler are logged and do not affect the menu.

### Opening menus

```java
MenuSession session = menu.open(player);                    // in front of the player, facing them
MenuSession session = menu.open(player, placeholders);      // with placeholders
MenuSession session = menu.openAt(player, center, placeholders); // at a fixed place in the world
```

- `open` places the screen `distance` blocks in front of the player's eyes, at eye height, facing them. It stays
  there: it does not follow the player.
- `openAt` places the center of the screen at `center`. The screen faces the direction of `center`'s yaw, so a
  player looking at it has the opposite yaw. Use this for menus that are part of the world, like a board in a lobby.
  Pass `TagResolver.empty()` if you do not need placeholders.
- The player must already be in an instance. Open menus from `PlayerSpawnEvent` or later, not during configuration.
- A player can have several menus open at once. Clicks and hover go to the closest screen they are looking at.

### Placeholders

Text content is MiniMessage, so you can pass any MiniMessage `TagResolver` when opening a menu:

```html
<text>Welcome back, <aqua><player></aqua>! You have <coins> coins.</text>
```

```java
menu.open(player, TagResolver.resolver(
        Placeholder.unparsed("player", player.getUsername()),
        Placeholder.component("coins", Component.text(coins, NamedTextColor.GOLD))
));
```

The layout is calculated with the resolved text, so long placeholder values are measured and wrapped correctly.

### Menu sessions

`open` returns a `MenuSession`: one menu shown to one player.

| Method                          | Description                                                                                 |
|---------------------------------|---------------------------------------------------------------------------------------------|
| `close()`                       | Removes the menu from the player's view. Safe to call more than once                        |
| `isOpen()`                      | Whether the menu is still shown                                                             |
| `setText(id, Component)`        | Replaces the content of `<text id="...">`. Returns `false` if there is no such element      |
| `setText(id, String)`           | Same, with MiniMessage input: `session.setText("score", "<gold>42")`                        |
| `player()` / `menu()`           | The viewer and the menu template                                                            |
| `position()`                    | The center of the screen in the world (its yaw is the facing direction)                      |

```java
Eyeliner.openMenus(player);   // all open sessions of a player
Eyeliner.closeAll(player);    // close all of them
```

Sessions are also closed automatically when the player:

- disconnects,
- changes instance,
- walks further away than `closeDistance` (if it is set on the `<screen>`).

`setText` does not recalculate the layout. Keep the new text about the same size as the old one, or close the
menu and open it again.

### Custom variants

Register your own variants with the same attributes you would write in a file:

```java
// For buttons only
Eyeliner.registerVariant("button", "gold", Map.of(
        "bg", "#f8a40799",
        "hoverBg", "#f8a407",
        "color", "#1a1a1a",
        "radius", "50%",
        "sound", "minecraft:entity.player.levelup",
        "soundPitch", "1.5"
));

// For every element
Eyeliner.registerVariant(StyleSheet.ANY, "glass", Map.of(
        "bg", "#ffffff20",
        "border", "0.01",
        "borderColor", "#ffffff60"
));
```

```html
<button variant="gold" onClick="vip">VIP</button>
<screen variant="glass rounded">...</screen>
```

Registering a variant with an existing name replaces it, including the built-in ones. A variant registered for a
specific tag takes priority over a variant with the same name registered for all elements.

---

## Player interaction

| Player action                | Result                                                                    |
|------------------------------|---------------------------------------------------------------------------|
| Look at a button             | The button shows its `hoverBg`                                             |
| Left click while looking at it  | Runs `onLeftClick`, or `onClick` if that is not set                     |
| Right click while looking at it | Runs `onRightClick`, or `onClick` if that is not set                    |

- Clicks work with an empty hand and while holding items.
- Only the front of a screen can be clicked. Looking at it from behind does nothing.
- Buttons can be used up to `reach` blocks away (default 6).
- One physical click can send several packets to the server; Eyeliner counts it as one click (clicks within
  150 ms are merged).
- Invisible interaction entities in front of each button stop clicks from breaking or using the block behind it,
  as long as the button is within the player's normal entity reach (3 blocks in survival).

---

## Errors and warnings

**Errors** stop loading and throw an `EyelinerParseException` with the line and column:

- malformed markup (unclosed tags, mismatched closing tags, unterminated attribute values or comments)
- a root element other than `<screen>`, or more than one root element
- unknown elements, or elements nested where they are not allowed
- duplicate attributes on one element

```
main.eyeliner: <button> is not allowed inside <button> (allowed: text, sprite) (line 12, column 9)
```

**Warnings** do not stop loading. The value falls back to its default and a message is logged with the logger
`Eyeliner`. They are also available as a list from `menu.warnings()`:

- unknown attributes (usually typos)
- invalid values, for example `bg="#zzz"`, `width="wide"` or `item="minecraft:not_an_item"`
- unknown variants

```
Invalid value '#zzz' for 'bg' on <screen> at 1:1 (not a color: #zzz), using default '#14141ee6'
Unknown attribute 'colr' on <screen> at 1:1
```

---

## Limitations

- Screens are always upright. When opened, they turn to face the player left/right but do not tilt up or down.
- `setText` does not recalculate the layout.
- Content that does not fit inside its parent is not clipped.
- Text width is measured with the default Minecraft font. Custom fonts from resource packs and some non-Latin
  characters can be measured slightly wrong.
- If a player walks far enough away that the chunk with the menu unloads on their client, the menu disappears
  for them. The session stays open on the server until it is closed.
- Every background piece, text, sprite and button click area is a separate entity. Rounded corners and wide
  buttons use more entities than square corners and small buttons. The full example menu uses about 100 entities
  per player.
