# UML Editor — 程式架構說明

## 目錄

1. [整體架構概覽](#1-整體架構概覽)
2. [套件結構](#2-套件結構)
3. [設計模式](#3-設計模式)
4. [各層詳細說明](#4-各層詳細說明)
   - [4.1 進入點 — `uml.app`](#41-進入點--umlapp)
   - [4.2 資料模型層 — `uml.model`](#42-資料模型層--umlmodel)
   - [4.3 使用者介面層 — `uml.ui`](#43-使用者介面層--umlui)
   - [4.4 工具模式層 — `uml.tool`](#44-工具模式層--umltool)
   - [4.5 工具函式層 — `uml.util`](#45-工具函式層--umlutil)
5. [資料流與事件流](#5-資料流與事件流)
6. [繪製層次](#6-繪製層次)
7. [類別相依圖](#7-類別相依圖)

---

## 1. 整體架構概覽

本專案採用 **MVC（Model-View-Controller）** 架構，搭配 **Strategy 模式** 處理工具切換：

```
┌─────────────────────────────────────────────────────┐
│                     uml.app                         │
│                  Main（進入點）                       │
└────────────────────────┬────────────────────────────┘
                         │ 建立
          ┌──────────────▼──────────────┐
          │          uml.ui             │
          │  MainFrame / CanvasPanel    │  ◄── View
          │  ToolBar / LabelDialog      │
          └───────┬──────────┬──────────┘
                  │ 持有      │ 委派滑鼠事件
          ┌───────▼──────┐  ┌▼──────────────┐
          │  uml.model   │  │   uml.tool    │
          │ DiagramModel │  │  ToolMode     │  ◄── Controller
          │ GraphicObject│  │  SelectMode   │
          │ Link / ...   │  │  AbstractLink │
          └───────┬──────┘  └───────────────┘
                  │ 使用
          ┌───────▼──────┐
          │   uml.util   │
          │  GeomUtils   │
          │  PortUtils   │
          └──────────────┘
```

---

## 2. 套件結構

```
src/
└── uml/
    ├── app/
    │   └── Main.java                  進入點
    ├── model/
    │   ├── GraphicObject.java         所有圖形物件的抽象基底類別
    │   ├── BasicObject.java           單一（非複合）圖形的抽象基底
    │   ├── RectObject.java            矩形圖形
    │   ├── OvalObject.java            橢圓圖形
    │   ├── CompositeObject.java       複合（群組）圖形
    │   ├── Link.java                  UML 關聯線
    │   ├── LinkType.java              關聯線類型列舉
    │   └── DiagramModel.java          圖表的唯一資料來源
    ├── ui/
    │   ├── MainFrame.java             應用程式主視窗
    │   ├── CanvasPanel.java           繪圖畫布
    │   ├── ToolBar.java               左側工具列
    │   └── LabelDialog.java           標籤編輯對話框
    ├── tool/
    │   ├── ToolMode.java              工具模式介面（Strategy 介面）
    │   ├── SelectMode.java            選取 / 移動 / 縮放 工具
    │   ├── RectMode.java              矩形建立工具（暫態）
    │   ├── OvalMode.java              橢圓建立工具（暫態）
    │   ├── AbstractLinkMode.java      關聯線建立工具的共用基底
    │   ├── AssociationMode.java       關聯（Association）工具
    │   ├── GeneralizationMode.java    一般化（Generalization）工具
    │   └── CompositionMode.java       組合（Composition）工具
    └── util/
        ├── GeomUtils.java             幾何運算工具函式
        └── PortUtils.java             連接埠位置計算工具函式
```

---

## 3. 設計模式

### 3.1 Strategy 模式（工具切換）

`ToolMode` 是工具行為的 Strategy 介面。`CanvasPanel` 持有一個 `activeMode` 參考，所有滑鼠事件都轉發給它。切換工具只需要呼叫 `canvas.setActiveTool(newMode)`，`CanvasPanel` 本身完全不需要 `if/switch` 判斷目前是哪種工具。

```
ToolMode（介面）
  ├── SelectMode
  ├── RectMode
  ├── OvalMode
  └── AbstractLinkMode（抽象類別）
        ├── AssociationMode
        ├── GeneralizationMode
        └── CompositionMode
```

**關鍵程式碼：**
- 介面定義：`src/uml/tool/ToolMode.java:19`
- 事件轉發：`src/uml/ui/CanvasPanel.java:76-91`
- 工具切換：`src/uml/ui/CanvasPanel.java:102-107`

---

### 3.2 Composite 模式（圖形物件層次）

圖形物件採用 Composite 模式，讓 `CanvasPanel` 可以用同樣的方式對待單一物件與群組物件。

```
GraphicObject（抽象，Component）
  ├── BasicObject（抽象，Leaf 基底）
  │     ├── RectObject
  │     └── OvalObject
  └── CompositeObject（Composite）
        └── children: List<GraphicObject>（可嵌套）
```

`DiagramModel.getObjects()` 回傳的列表只包含頂層的 `GraphicObject`，畫布呼叫 `obj.draw(g)` 不需要知道物件是 `BasicObject` 還是 `CompositeObject`。

**關鍵程式碼：**
- 抽象基底：`src/uml/model/GraphicObject.java:17`
- Composite 繪製：`src/uml/model/CompositeObject.java:85-101`
- 畫布渲染迴圈：`src/uml/ui/CanvasPanel.java:263-265`

---

### 3.3 MVC 模式

| 角色 | 類別 | 說明 |
|------|------|------|
| Model | `DiagramModel` | 維護所有 `GraphicObject` 與 `Link` 的清單 |
| View | `CanvasPanel` | 讀取 Model 資料並繪製；不含任何互動邏輯 |
| Controller | `ToolMode` 各實作 | 接收滑鼠事件，修改 Model，通知 View 重繪 |

---

### 3.4 Lazy-update 模式（連接線自動跟隨）

`Link` 不儲存起點/終點座標，每次 `draw()` 時都從 `source.getPorts()` 和 `target.getPorts()` 動態計算。因此當連接的物件移動或縮放後，下一次重繪時連接線會自動跟隨，不需要任何監聽器或通知機制。

**關鍵程式碼：** `src/uml/model/Link.java:65-67`

---

## 4. 各層詳細說明

### 4.1 進入點 — `uml.app`

#### `Main.java`

```java
// src/uml/app/Main.java:13-15
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
}
```

唯一的職責：在 Swing 的 Event Dispatch Thread (EDT) 上建立 `MainFrame`。
Swing 要求所有 UI 元件的建立與修改都必須在 EDT 上執行，`invokeLater` 確保這一點。

---

### 4.2 資料模型層 — `uml.model`

這一層是整個應用程式的資料核心，不依賴任何 UI 套件（除了 `java.awt` 的幾何類別）。

---

#### `GraphicObject.java` — 所有圖形的抽象根

```java
// src/uml/model/GraphicObject.java:17
public abstract class GraphicObject {
    public abstract void draw(Graphics2D g);
    public abstract boolean contains(Point p);
    public abstract Rectangle getBoundingBox();
    public abstract String getLabel();
    public abstract Color getLabelColor();
}
```

定義了所有圖形物件必須實作的合約：
- `draw(g)` — 將自身繪製到畫布上
- `contains(p)` — 判斷點 `p` 是否在此物件內（用於點擊測試）
- `getBoundingBox()` — 回傳包圍矩形（用於選取、群組、Z 排序）
- `getLabel()` / `getLabelColor()` — 標籤文字與填色

---

#### `BasicObject.java` — 單一圖形的抽象基底

```java
// src/uml/model/BasicObject.java:21
public abstract class BasicObject extends GraphicObject {
    protected int x, y, width, height;
    protected String label      = "";
    protected Color  labelColor = Color.WHITE;
    // ...
    public abstract List<Point> getPorts();
}
```

封裝了所有「單一圖形」共有的狀態（位置、大小、標籤），讓 `RectObject` 和 `OvalObject` 只需要負責繪製外型與定義連接埠。

重要方法：
- `moveTo(newX, newY)` — 絕對座標移動（`src/uml/model/BasicObject.java:78`）
- `resize(r)` — 套用新的包圍矩形，最小尺寸限制 20×20 px（`src/uml/model/BasicObject.java:97-102`）
- `getPorts()` — 抽象方法，由子類別透過 `PortUtils` 實作

---

#### `RectObject.java` — 矩形圖形

```java
// src/uml/model/RectObject.java:31-49
public void draw(Graphics2D g) {
    g.setColor(labelColor);
    g.fillRect(x, y, width, height);       // 1. 填色
    g.setColor(Color.BLACK);
    g.drawRect(x, y, width, height);       // 2. 黑色邊框
    if (!label.isEmpty()) {
        // 3. 置中標籤文字
    }
}

public List<Point> getPorts() {
    return PortUtils.computeRectPorts(getBoundingBox());  // 8 個連接埠
}
```

提供 8 個連接埠（四角 + 四邊中點），從左上角順時針編號 0~7。

```
0(TL)  1(TM)  2(TR)
7(ML)         3(MR)
6(BL)  5(BM)  4(BR)
```

---

#### `OvalObject.java` — 橢圓圖形

與 `RectObject` 結構相同，差異在於：
- 使用 `fillOval` / `drawOval` 繪製
- 只有 4 個連接埠（上、右、下、左）

```
     0(top)
3(left)   1(right)
     2(bottom)
```

**關鍵程式碼：** `src/uml/model/OvalObject.java:56-59`

---

#### `CompositeObject.java` — 複合（群組）圖形

```java
// src/uml/model/CompositeObject.java:63-70
public Rectangle getBoundingBox() {
    Rectangle bbox = null;
    for (GraphicObject child : children) {
        Rectangle cb = child.getBoundingBox();
        bbox = (bbox == null) ? new Rectangle(cb) : GeomUtils.union(bbox, cb);
    }
    return (bbox != null) ? bbox : new Rectangle(0, 0, 0, 0);
}
```

- **包圍矩形**：動態計算，等於所有子物件包圍矩形的聯集。每次呼叫都重新計算，因此物件移動後不需要更新。
- **繪製**：先畫所有子物件，再疊上虛線框作為群組識別（`src/uml/model/CompositeObject.java:85-101`）
- **移動**：`moveTo(newX, newY)` 計算位移量並遞迴傳播給所有子物件（`src/uml/model/CompositeObject.java:133-157`）
- **不支援 resize**：規格要求群組物件不可調整大小

---

#### `Link.java` — UML 關聯線

```java
// src/uml/model/Link.java:29-45
public class Link {
    private final LinkType    type;
    private final BasicObject source;
    private final int         sourcePortIndex;
    private final BasicObject target;
    private final int         targetPortIndex;
}
```

儲存「哪個物件的哪個連接埠」連到「哪個物件的哪個連接埠」，以及連線類型。

`draw()` 方法根據 `type` 繪製不同的箭頭樣式：

| `LinkType` | 箭頭 | 位置 |
|------------|------|------|
| `ASSOCIATION` | 開放 V 形箭頭 | 目標端 |
| `GENERALIZATION` | 空心三角形 | 目標端（父類別端） |
| `COMPOSITION` | 填滿黑色菱形 | 來源端（擁有者端） |

**關鍵程式碼：**
- 箭頭分派：`src/uml/model/Link.java:88-98`
- Association 箭頭：`src/uml/model/Link.java:109-122`
- Generalization 三角形：`src/uml/model/Link.java:130-152`
- Composition 菱形：`src/uml/model/Link.java:161-183`

---

#### `LinkType.java` — 關聯線類型列舉

```java
// src/uml/model/LinkType.java:10-14
public enum LinkType {
    ASSOCIATION,
    GENERALIZATION,
    COMPOSITION
}
```

新增關聯線類型只需在此加一個常數，再在 `Link.draw()` 加一個 `case`，其他程式碼不需要修改（開放-封閉原則）。

---

#### `DiagramModel.java` — 圖表的唯一資料來源

```java
// src/uml/model/DiagramModel.java:23-27
public class DiagramModel {
    private final List<GraphicObject> objects = new ArrayList<>();  // Z 軸順序
    private final List<Link>          links   = new ArrayList<>();
}
```

`DiagramModel` 是整個圖表狀態的唯一擁有者，提供以下功能：

**物件管理：**
- `addObject(obj)` / `removeObject(obj)` — 新增/移除物件（`src/uml/model/DiagramModel.java:31-38`）
- `getObjects()` — 回傳唯讀視圖（`src/uml/model/DiagramModel.java:45-47`）
- `moveToFront(obj)` — 將物件移至最前方（從清單移除再重新加入末端）（`src/uml/model/DiagramModel.java:57-60`）

**點擊測試（Hit-testing）：**
- `getObjectAt(p)` — 從前向後找第一個包含點 `p` 的物件（`src/uml/model/DiagramModel.java:77-83`）
- `getBasicObjectAt(p)` — 同上但只回傳 `BasicObject`（連線工具使用）（`src/uml/model/DiagramModel.java:91-99`）

**連線管理：**
- `addLink(link)` / `removeLink(link)` / `getLinks()` — 關聯線 CRUD（`src/uml/model/DiagramModel.java:104-119`）

**群組操作：**
- `getObjectsCompletelyInside(r)` — 找出完全在矩形 `r` 內的所有物件（框選使用）（`src/uml/model/DiagramModel.java:136-144`）
- `group(selected)` — 將選取物件包成 `CompositeObject`，加入模型並回傳（`src/uml/model/DiagramModel.java:160-169`）
- `ungroup(composite)` — 解散複合物件，子物件回到頂層清單（`src/uml/model/DiagramModel.java:187-193`）

---

### 4.3 使用者介面層 — `uml.ui`

#### `MainFrame.java` — 應用程式主視窗

```java
// src/uml/ui/MainFrame.java:31-48
public MainFrame() {
    model   = new DiagramModel();
    canvas  = new CanvasPanel(model);
    toolBar = new ToolBar(canvas);

    setLayout(new BorderLayout());
    add(canvas,  BorderLayout.CENTER);  // 畫布佔中央
    add(toolBar, BorderLayout.WEST);    // 工具列在左側
    setJMenuBar(buildMenuBar());        // 選單列在頂部
}
```

負責：
1. 建立並持有 `DiagramModel`（整個應用程式只有一個）
2. 組裝版面配置：工具列（左）+ 畫布（中）+ 選單列（上）
3. 處理 **Edit 選單** 的三個動作：

| 選單項目 | 方法 | 條件 |
|----------|------|------|
| Group | `onGroup()` | 需選取 ≥ 2 個物件 |
| Ungroup | `onUngroup()` | 需選取 1 個 `CompositeObject` |
| Label | `onLabel()` | 需選取 1 個 `BasicObject` |

**關鍵程式碼：**
- Group 動作：`src/uml/ui/MainFrame.java:77-84`
- Ungroup 動作：`src/uml/ui/MainFrame.java:97-105`
- Label 動作：`src/uml/ui/MainFrame.java:119-136`

---

#### `CanvasPanel.java` — 繪圖畫布

這是最核心的 UI 元件，但**不包含任何互動邏輯**——所有互動都委派給 `activeMode`。

**職責：**
1. 持有 `DiagramModel` 參考，用於渲染
2. 持有 `activeMode`（當前工具），轉發所有滑鼠事件
3. 維護暫態覆蓋層狀態（預覽框、連線預覽、框選矩形）
4. 在 `paintComponent` 中按照正確的層次順序繪製所有東西

**滑鼠事件轉發：**
```java
// src/uml/ui/CanvasPanel.java:76-88
MouseAdapter adapter = new MouseAdapter() {
    public void mousePressed (MouseEvent e) { activeMode.onMousePressed(e, model, CanvasPanel.this); }
    public void mouseDragged (MouseEvent e) { activeMode.onMouseDragged(e, model, CanvasPanel.this); }
    public void mouseReleased(MouseEvent e) { activeMode.onMouseReleased(e, model, CanvasPanel.this); }
    public void mouseMoved   (MouseEvent e) { activeMode.onMouseMoved(e, model, CanvasPanel.this); }
};
```

**暫態狀態欄位：**

| 欄位 | 類型 | 用途 |
|------|------|------|
| `previewBounds` | `Rectangle` | 建立圖形時的拖曳預覽框 |
| `hoveredObject` | `GraphicObject` | 滑鼠懸停的物件（顯示連接埠） |
| `selection` | `List<GraphicObject>` | 目前選取的物件清單 |
| `linkPreviewStart/End` | `Point` | 建立連線時的虛線預覽 |
| `dragRect` | `Rectangle` | 框選拖曳的藍色虛線矩形 |

**工具切換邏輯（Transient 工具）：**
```java
// src/uml/ui/CanvasPanel.java:102-107
public void setActiveTool(ToolMode mode) {
    if (activeMode != null && !activeMode.isTransient()) {
        previousMode = activeMode;  // 只儲存「持久性」工具
    }
    activeMode = mode;
}
```
`RectMode` 和 `OvalMode` 的 `isTransient()` 回傳 `true`，所以它們不會覆蓋 `previousMode`。畫完圖形後，這些工具會自動回到 `previousMode`（通常是 `SelectMode`）。

---

#### `ToolBar.java` — 左側工具列

```java
// src/uml/ui/ToolBar.java:40-43
private static final String[] LABELS = {
    "Select", "Association", "Generalization", "Composition", "Rect", "Oval"
};
```

6 個 `JToggleButton` 加入同一個 `ButtonGroup`，確保同時只有一個按鈕被選取。
點擊按鈕時：
1. 更新按鈕的視覺高亮（深色背景）
2. 呼叫 `canvas.setActiveTool(modes[idx])` 切換工具

**關鍵程式碼：**
- 工具陣列建立：`src/uml/ui/ToolBar.java:95-104`
- 按鈕事件處理：`src/uml/ui/ToolBar.java:55-58`

---

#### `LabelDialog.java` — 標籤編輯對話框

Modal 對話框，讓使用者編輯 `BasicObject` 的標籤文字和填色。

```java
// src/uml/ui/LabelDialog.java:58-127
public LabelDialog(Frame owner, String initialName, Color initialColor) {
    super(owner, "Customize Label Style", true /* modal */);
    // ...
    setVisible(true);  // 阻塞 EDT 直到使用者關閉對話框
}
```

**設計特點：**
- 建構函式結束前就呼叫 `setVisible(true)`，因此建構函式會阻塞直到使用者按下 OK 或 Cancel
- 不直接修改 Model，呼叫端（`MainFrame.onLabel()`）在確認後自行套用變更
- 即時顏色預覽：使用者用 `JColorChooser` 選色後，色票 panel 立刻更新背景色

**關鍵程式碼：**
- 顏色選擇：`src/uml/ui/LabelDialog.java:75-82`
- 確認/取消：`src/uml/ui/LabelDialog.java:110-111`

---

### 4.4 工具模式層 — `uml.tool`

#### `ToolMode.java` — Strategy 介面

```java
// src/uml/tool/ToolMode.java:19-37
public interface ToolMode {
    void onMousePressed (MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseDragged (MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseMoved   (MouseEvent e, DiagramModel model, CanvasPanel canvas);

    default boolean isTransient() { return false; }
}
```

每個方法都收到相同的三個參數，讓工具可以讀取/修改模型、通知畫布重繪，而不需要全域參考。`isTransient()` 預設回傳 `false`，只有 `RectMode`/`OvalMode` 覆寫為 `true`。

---

#### `SelectMode.java` — 選取/移動/縮放工具

這是最複雜的工具，內部有一個狀態機：

```
IDLE
 ├─ 按在連接埠上 → RESIZING
 ├─ 按在物件上  → MOVING
 └─ 按在空白處  → DRAG_SELECTING
```

**三個優先順序分支（`onMousePressed`）：**

```java
// src/uml/tool/SelectMode.java:127-204
// Branch 1: 連接埠命中 → 縮放
if (hovered instanceof BasicObject) {
    // 對每個連接埠做距離測試（半徑 8px）
    if (dx*dx + dy*dy <= PORT_RADIUS_SQ) { state = RESIZING; return; }
}
// Branch 2: 物件命中 → 移動
GraphicObject hit = model.getObjectAt(e.getPoint());
if (hit != null) { state = MOVING; return; }
// Branch 3: 空白 → 框選
state = DRAG_SELECTING;
```

**縮放邏輯（`onMouseDragged` — RESIZING 狀態）：**

連接埠分為三種：
- **角連接埠**（TL/TR/BL/BR）：X 和 Y 軸都可自由變動
- **上/下邊中點**（TM/BM）：只有 Y 軸可變，X 和寬度固定
- **左/右邊中點**（ML/MR）：只有 X 軸可變，Y 和高度固定

縮放時以**對角連接埠**為固定錨點（`resizeAnchorPoint`），確保拖曳時對面的角/邊不會移動。

**關鍵程式碼：**
- 連接埠命中偵測：`src/uml/tool/SelectMode.java:136-173`
- 縮放拖曳：`src/uml/tool/SelectMode.java:210-240`
- 移動拖曳：`src/uml/tool/SelectMode.java:242-260`
- 框選釋放：`src/uml/tool/SelectMode.java:284-294`

---

#### `RectMode.java` / `OvalMode.java` — 圖形建立工具（暫態）

兩者邏輯完全相同，只差在建立的物件類型：

```
按下滑鼠 → 記錄起點
拖曳      → 更新預覽框（虛線輪廓）
放開滑鼠 → 建立 RectObject/OvalObject，清除預覽，切回前一個工具
```

```java
// src/uml/tool/RectMode.java:40-53
public void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
    Rectangle bounds = GeomUtils.normalizeRect(pressPoint, e.getPoint());
    bounds.width  = Math.max(bounds.width,  20);  // 最小 20px
    bounds.height = Math.max(bounds.height, 20);
    model.addObject(new RectObject(bounds));
    canvas.clearPreviewBounds();
    canvas.setActiveTool(canvas.getPreviousMode());  // 切回前一個工具
    canvas.repaint();
}
```

---

#### `AbstractLinkMode.java` — 關聯線建立工具的共用基底

三種連線工具（Association/Generalization/Composition）互動邏輯完全相同，只有箭頭樣式不同。`AbstractLinkMode` 實作所有邏輯，子類別只需覆寫 `getLinkType()`：

```java
// src/uml/tool/AssociationMode.java
public class AssociationMode extends AbstractLinkMode {
    protected LinkType getLinkType() { return LinkType.ASSOCIATION; }
}
```

狀態機：
```
IDLE
 ├─ 按在 BasicObject 上 → DRAWING（顯示虛線預覽）
 └─ 按在空白/複合物件上 → INVALID

DRAWING
 ├─ 拖曳 → 更新預覽線終點
 └─ 放開
      ├─ 在不同 BasicObject 上 → 建立 Link，回到 IDLE
      └─ 在空白/同一物件上   → 放棄，回到 IDLE
```

連線的起點/終點連接埠由 `GeomUtils.nearestPortIndex` 自動選取最近的連接埠，使用者只需在物件上按下/放開即可。

**關鍵程式碼：**
- 按下處理：`src/uml/tool/AbstractLinkMode.java:63-76`
- 釋放建立：`src/uml/tool/AbstractLinkMode.java:103-125`

---

### 4.5 工具函式層 — `uml.util`

這一層是純函式（Stateless），所有方法都是 `static`，不持有任何狀態。

#### `PortUtils.java` — 連接埠位置計算

```java
// src/uml/util/PortUtils.java:32-48
public static List<Point> computeRectPorts(Rectangle bbox) {
    // 回傳 8 個連接埠：TL TM TR MR BR BM BL ML
}

public static List<Point> computeOvalPorts(Rectangle bbox) {
    // 回傳 4 個連接埠：top right bottom left
}
```

連接埠位置完全由包圍矩形推算，不儲存任何狀態。`BasicObject` 子類別在 `getPorts()` 中呼叫這裡的方法。

---

#### `GeomUtils.java` — 幾何運算工具函式

三個靜態方法：

```java
// src/uml/util/GeomUtils.java

// 1. 找最近連接埠索引（避免 sqrt，用距離平方比較）
public static int nearestPortIndex(List<Point> ports, Point p)

// 2. 兩個矩形的聯集（CompositeObject 計算包圍框用）
public static Rectangle union(Rectangle a, Rectangle b)

// 3. 正規化矩形（確保 width/height 恆為正，向任意方向拖曳都正確）
public static Rectangle normalizeRect(Point p1, Point p2)
```

`normalizeRect` 是被使用次數最多的工具函式，`RectMode`、`OvalMode`、`SelectMode`（框選）都使用它。

---

## 5. 資料流與事件流

### 5.1 建立圖形的完整流程（以矩形為例）

```
使用者點擊「Rect」按鈕
  → ToolBar.ActionListener
  → canvas.setActiveTool(new RectMode())

使用者在畫布上按下滑鼠
  → CanvasPanel.mousePressed
  → RectMode.onMousePressed → 記錄 pressPoint

使用者拖曳
  → CanvasPanel.mouseDragged
  → RectMode.onMouseDragged
  → canvas.setPreviewBounds(normalizeRect(...), false)
  → canvas.repaint()
  → CanvasPanel.paintComponent → 繪製虛線預覽框

使用者放開滑鼠
  → CanvasPanel.mouseReleased
  → RectMode.onMouseReleased
  → model.addObject(new RectObject(bounds))  ← 修改 Model
  → canvas.clearPreviewBounds()
  → canvas.setActiveTool(previousMode)        ← 切回 SelectMode
  → canvas.repaint()
  → CanvasPanel.paintComponent → 繪製新矩形
```

### 5.2 移動物件的完整流程

```
使用者按下物件
  → SelectMode.onMousePressed (Branch 2)
  → model.moveToFront(hit)         ← 物件移至最前
  → 記錄 moveOffset                ← 儲存滑鼠相對物件的偏移
  → state = MOVING

使用者拖曳
  → SelectMode.onMouseDragged
  → hit.moveTo(e.getX() - moveOffset.x, e.getY() - moveOffset.y)  ← 修改 Model
  → canvas.repaint()

使用者放開
  → SelectMode.onMouseReleased
  → state = IDLE
```

---

## 6. 繪製層次

`CanvasPanel.paintComponent` 按照以下順序繪製，確保視覺層次正確：

```
Layer 1: 白色背景（super.paintComponent）
Layer 2: 所有 GraphicObject（依 Z 軸順序，index 0 在最後面）
Layer 3: 懸停/選取指示器（連接埠方塊 或 藍色外框）
Layer 4: 所有 Link（連線畫在圖形上面）
Layer 5: 連線建立預覽（虛線，跟隨滑鼠）
Layer 6: 圖形建立預覽（虛線輪廓，跟隨滑鼠）
Layer 7: 框選矩形（藍色虛線，最上層）
```

**關鍵程式碼：** `src/uml/ui/CanvasPanel.java:247-328`

---

## 7. 類別相依圖

```
Main
 └── MainFrame
       ├── DiagramModel
       │     ├── GraphicObject (abstract)
       │     │     ├── BasicObject (abstract)
       │     │     │     ├── RectObject ──→ PortUtils
       │     │     │     └── OvalObject ──→ PortUtils
       │     │     └── CompositeObject ──→ GeomUtils
       │     └── Link
       │           └── LinkType (enum)
       ├── CanvasPanel
       │     └── ToolMode (interface)
       │           ├── SelectMode ──→ GeomUtils
       │           ├── RectMode   ──→ GeomUtils
       │           ├── OvalMode   ──→ GeomUtils
       │           └── AbstractLinkMode ──→ GeomUtils
       │                 ├── AssociationMode
       │                 ├── GeneralizationMode
       │                 └── CompositionMode
       ├── ToolBar
       └── LabelDialog
```

**依賴規則：**
- `uml.tool` 依賴 `uml.model` 和 `uml.ui`（需要修改 Model、通知 Canvas）
- `uml.ui` 依賴 `uml.model`（需要讀取 Model 渲染）和 `uml.tool`（持有 ToolMode 參考）
- `uml.model` **不依賴** `uml.ui` 或 `uml.tool`（Model 層保持純淨）
- `uml.util` **不依賴** 任何其他套件（純函式工具層）
