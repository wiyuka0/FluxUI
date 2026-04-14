
# FluxUI 开发者使用文档

## 1. 快速起步

### 1.1 初始化底层渲染器
在插件或游戏启动时，你需要向 Flux 注册你自己的渲染逻辑。
```java
Flux.setPoolFactory(location -> new Flux.PoolImpl() {
    @Override
    public void poolBeginFrame() { /* 每帧开始清理或重置实体 */ }
    @Override
    public void poolEndFrame() { /* 每帧结束删除未使用的实体 */ }
    @Override
    public void poolDestroy() { /* 销毁整个 UI 时调用 */ }
    
    @Override // 文本渲染重载 1
    public void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxBillboard billboard, boolean seeThrough, Set<UUID> viewers) { /* 实现基础文本渲染 */ }
    
    @Override // 文本渲染重载 2 (带对齐方式)
    public void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxTextAlignment alignment, Flux.FluxBillboard billboard, boolean seeThrough, Set<UUID> viewers) { /* 实现对齐文本渲染 */ }
    
    @Override
    public void poolDrawRect(String id, Matrix4d worldTransform, Flux.FluxColor color, int interpTicks, Flux.FluxBillboard billboard, boolean seeThrough, Set<UUID> viewers) { /* 实现矩形块渲染 */ }
    
    @Override
    public void poolDrawTriangle(String id, Vector3d point1, Vector3d point2, Vector3d point3, Matrix4d worldBaseMatrix, Flux.FluxColor color, int interpTicks, Flux.FluxBillboard billboard, boolean seeThrough, Set<UUID> viewers) { /* 实现三角形渲染 */ }
});
```

### 1.2 创建和销毁实例
为你的玩家或特定的交互场景创建一个 `Flux` 实例：
```java
UUID playerId = player.getUniqueId();
Flux ui = new Flux(playerId, flux -> {
    // 这里编写你的 UI 渲染代码，切记不要在这里内嵌使用 flux.tick() 方法
    renderMyUI(flux);
});

// 在不需要时销毁以回收资源
ui.destroy();
```

### 1.3 驱动更新 (Tick)
在主循环或定时任务中，需要调用 `tick()`，并更新玩家的视线以响应交互：

```java
// 1. 每 tick 更新玩家视线（用于判断鼠标指针悬停/点击）
ui.updatePlayerRay(playerId, playerEyeLocation, playerDirection);

// 2. 如果玩家这一帧按下了交互键(如左/右键)，注册点击
// 框架会在本帧渲染并判定点击事件后自动清除该状态，所以每帧按需传入即可
if (playerClickedThisTick) {
    ui.registerPlayerClick(playerId);
}

// 3. 驱动 UI 开始计算排版和渲染
ui.tick();
```

---

## 2. 编写 UI 界面

在 `Flux` 的回调函数或每帧执行的代码中，使用以下结构来绘制面板。

### 2.1 创建屏幕 (Screen) 与窗口 (Window)
必须先定义一个 3D 屏幕位置，然后才能开始绘制窗口。

```java
public void renderMyUI(Flux flux) {
    // 1. 定义屏幕的中心位置
    // 第五个参数(可选)可填入玩家的UUID以骑乘功能： new Flux.FluxLocation("world", 10.5, 65.0, 20.5, entityUuid);
    Flux.FluxLocation loc = new Flux.FluxLocation("world", 10.5, 65.0, 20.5); 
    
    Vector3d xAxis = new Vector3d(1, 0, 0); // 屏幕的本地 X 轴
    Vector3d yAxis = new Vector3d(0, 1, 0); // 屏幕的本地 Y 轴
    Vector3d zAxis = new Vector3d(0, 0, 1); // 屏幕的正 Z 轴（朝向向量）
    
    // 2. 开启一个 Screen 作用域
    if (flux.screen(loc, xAxis, yAxis, zAxis, "my_main_screen")) {
        
        // 3. 开启一个自动排版的 Window (原点为相对左上角的 0,0)
        flux.beginWindow("系统设置", 0, 0);
        
        // 这里添加你的 UI 控件
        
        flux.endWindow(); // 结束窗口包裹层
        flux.endScreen(); // 结束并弹出矩阵
    }
}
```

### 2.2 常用 UI 控件

**文本 (Text)**
```java
flux.text("欢迎来到服务器!");
```

**按钮 (Button)**
```java
if (flux.button("确认提交")) {
    player.sendMessage("你点击了确认！");
    // 执行提交逻辑...
}
```

**复选框 (Checkbox)**
```java
// myBooleanState 必须是外部维护的类变量，用来保存状态
myBooleanState = flux.checkbox("开启选项", myBooleanState);
```

**滑块 (Slider)**
```java
// volume 必须是外部维护的类变量。由于内容可能发生变动，必须显式指定ID
volume = flux.sliderFloat("sld_volume_0", "音量", volume, 0.0f, 100.0f);
```

### 2.3 布局控制
默认情况下，每次调用排版控件都会自动向下换一行绘制。你可以使用 `sameLine()` 让排版游标维持在同一行：
```java
flux.button("取消");
flux.sameLine();
flux.button("确定");
```

---

## 3. 进阶功能

### 3.1 UI 样式自定义 (FluxStyle)

```java
Flux.FluxStyle style = new Flux.FluxStyle();
style.textScale = 0.5f;
style.colButton = Flux.FluxColor.fromARGB(255, 200, 50, 50); // 红色按钮
style.windowPadding = new Vector2f(0.2f, 0.2f);

// 覆盖当前渲染框架的热拔插样式设定
flux.style(style);
```

### 3.2 矩阵变换 (Matrix Transforms)
FluxUI 使用矩阵栈控制坐标系，方便你制作局部的排版偏移或 3D 旋转：

```java
flux.pushMatrix();
flux.translate(1.0f, 0, 0); // 偏移位置
flux.rotateY(45f);          // 沿Y轴旋转 (注意：单位为角度制 Degree)
flux.scale(1.5f, 1.5f, 1.5f); // 基于局部放大

flux.button("带有独立变换的按钮");

flux.popMatrix(); // 恢复调用之前的变换状态
```

### 3.3 渲染参数选项
*   **Billboard:** 控制面板是否自动面朝玩家视角。
    `flux.text("跟随视角文本", Flux.FluxBillboard.CENTER);`
    可选值：`FIXED` (固定), `CENTER` (中心对齐), `HORIZONTAL` (水平面朝向), `VERTICAL` (垂直面朝向)。
*   **SeeThrough (透视):** 允许 UI 隔墙可见。该功能是否生效取决于外部游戏引擎/渲染池对透视渲染的具体实现。
    `flux.seeThrough(true);`
*   **Viewers:** 控制之后渲染生成的图形只能被特定的玩家看到。
    `flux.viewers(player1.getUniqueId(), player2.getUniqueId());`

框架内通过 **生成 ID (哈希)** 来追踪控件悬停、点击以及做实体复用。
直接传入字符串虽然方便（框架通过 `text.hashCode()` 自动生成 ID），但如果你的文本和内容**包含动态变量**，请务必手动传入固定 ID 参数。

```java
// 内容动态变化，会导致底层不断生成新的哈希ID并创建新实体，导致内存严重泄漏和屏幕闪烁
flux.text("次数: " + counts); 

// 显式固定组件唯一 ID (前缀参数)
flux.text("txt_count", "次数: " + counts);
```

---

## 4. 示例 (Example)

以下是一个在 Bukkit 服务器环境里的模板。

```java
import org.joml.Vector3d;
import org.bukkit.entity.Player;
import java.util.UUID;

public class FluxExampleManager {
    
    private Flux fluxUI;
    private boolean isFeatureEnabled = false;
    private float customVolume = 50.0f;
    private int clickCount = 0;

    public void openUIForPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 1. 创建 UI 实例并绑定玩家 ID
        this.fluxUI = new Flux(playerId, flux -> {
            Flux.FluxLocation loc = new Flux.FluxLocation(player.getWorld().getName(), 0, 100, 0);
            
            // 面向南方的基础轴
            Vector3d xAxis = new Vector3d(1, 0, 0); 
            Vector3d yAxis = new Vector3d(0, 1, 0);
            Vector3d zAxis = new Vector3d(0, 0, 1);

            // 该屏幕的起点位于世界坐标0 100 0
            if (flux.screen(loc, xAxis, yAxis, zAxis, "example_screen")) {
                flux.beginWindow("控制面板", 0, 0);
                
                // 固定 ID 文本渲染
                flux.text("txt_title", "点击数: " + clickCount);
                
                // 按钮交互
                if (flux.button("btn_click", "点我加一")) {
                    clickCount++;
                    player.playSound(player.getLocation(), "random.click", 1f, 1f);
                }
                
                // 复选框交互并更新自身状态
                isFeatureEnabled = flux.checkbox("chk_feature", "开启粒子特效", isFeatureEnabled);
                
                // 滑块调整数值
                customVolume = flux.sliderFloat("sld_volume", "音量调节", customVolume, 0f, 100f);
                
                if (flux.button("btn_cancel", "取消")) { /* 关闭逻辑 */ }
                flux.sameLine();
                if (flux.button("btn_confirm", "确认保存")) { /* 保存逻辑 */ }

                flux.endWindow();
                flux.endScreen();
            }
        });
    }

    public void tick(Player player, boolean isClickingRightMouse) {
        if (fluxUI == null) return;
        
        Vector3d eyePos = new Vector3d(player.getEyeLocation().getX(), player.getEyeLocation().getY(), player.getEyeLocation().getZ());
        Vector3d dir = new Vector3d(player.getLocation().getDirection().getX(), player.getLocation().getDirection().getY(), player.getLocation().getDirection().getZ());
        
        fluxUI.updatePlayerRay(player.getUniqueId(), eyePos, dir);
        
        if (isClickingRightMouse) {
            fluxUI.registerPlayerClick(player.getUniqueId());
        }
        
        fluxUI.tick();
    }
    
    public void closeUI() {
        if (fluxUI != null) {
            fluxUI.destroy();
            fluxUI = null;
        }
    }
}
```

---

## 5. API

### 5.1 生命周期与玩家输入
*   **`tick()`**: 驱动渲染框架的每帧渲染，调用绑定的回调逻辑。
*   **`destroy()`**: 销毁当前 Flux 实例及其实体池资源。
*   **`updatePlayerRay(UUID playerId, Vector3d origin, Vector3d direction)`**: 向系统输入玩家视野射线的原点和向量，提供指针交互计算的物理基础。
*   **`registerPlayerClick(UUID playerId)`**: 通知系统玩家按下了交互键，用于激发按钮和控件钩子。
*   **`removePlayer(UUID playerId)`**: 强制从输入系统的缓存中移除指定玩家。

### 5.2 屏幕与矩阵操作
*   **`boolean screen(FluxLocation loc, Vector3d xAxis, Vector3d yAxis, Vector3d zAxis, String screenId)`**:
    组建并挂载屏幕。
*   **`endScreen()`**:
    弹出屏幕所带来的矩阵系，结束本屏绘制。
*   **`pushMatrix()` / `popMatrix()`**:
    保存/恢复当前的矩阵栈环境。
*   **`translate(float x, float y, float z)`**:
    平移相对坐标系。
*   **`rotateX/Y/Z(float angle)`**:
    以**角度制(Degree)** 旋转坐标系。
*   **`scale(float x, float y, float z)`**:
    基于指定轴放缩视图大小。

### 5.3 窗口自动排版系统 (Auto layout)
*   **`beginWindow(String title, float startX, float startY)`**: 
    开启窗口，提供窗口标题。自 `startX` 和 `startY` 往下排版内部组件。可选的 `Billboard` 和 `textOpacity` 参数重载。其中textOpacity影响标题的透明度
*   **`endWindow()`**: 
    结束窗口
*   **`sameLine()`**: 
    将下一个控件渲染在原行的右侧。
*   **`style(FluxStyle style)`**: 
    更改组件间隙、按钮颜色特征。

### 5.4 交互组件与排版功能
带有 Auto-layout 与自适应交互能力的 UI 参数。（下列方法大多具有缺省首个参数 `String id` 的隐式生成同名方法）
*   **`void text(String id, String text)`**
*   **`boolean button(String id, String text)`**
*   **`boolean checkbox(String id, String label, boolean state)`**: 
    将当前的 state 变量传入判定，本帧被玩家点击则**取反后返回**。使用 `state = lux.checkbox(...)` 重新接受新的值。
*   **`float sliderFloat(String id, String label, float value, float min, float max)`**: 
*   **`void colorEdit3(String id, String label, FluxColor color)`**: 

### 5.5 基本图形绘制

#### 5.5.1 矩形
*   **`void rect(String id, float width, float height, FluxColor color)`**: 
    在当前变换矩阵的局部原点 `(0, 0, 0)` 处直接生成网格长宽。
*   **`void drawAbsRect(String id, float x, float y, float w, float h, FluxColor color)`**: 
    从局部偏移 `(x, y)` 构建面片宽高。

#### 5.5.2 三角形
*   **`void triangle(String id, Vector3d p1, Vector3d p2, Vector3d p3, FluxColor color)`**
*   **`void drawAbsTriangle(String id, float x1, float y1, float x2, float y2, float x3, float y3, FluxColor color)`**:
    通过六个浮点数（表征三个顶点的局部平面 2D 坐标）来构建三角形。

#### 5.5.3 自由排版
在相对于当前矩阵栈的坐标系下，使用给定的绝对 x、y 坐标生成可交互控件：
*   **`boolean buttonAbs(String id, String text, float x, float y)`**
*   **`boolean checkboxAbs(String id, String label, boolean state, float x, float y)`**
*   **`float sliderFloatAbs(String id, String label, float value, float min, float max, float x, float y)`**
*   **`void textAbs(String id, String text, float x, float y, float scale, int opacity, FluxTextAlignment align)`**: 提供详细参数实现完全控制的文本能力。

#### 5.5.4 渲染管线参数
*   **`void seeThrough(boolean seeThrough)`**: 启用/废除随后的组件进行深度测试（类似于 X-Ray，将使其隔墙透视）。
*   **`void viewers(UUID... viewers)` / `void clearViewers()`**: 过滤随后调用的实体更新发向客户端网络层的名单白名单。调用 `clearViewers` 重置回全局广播模式。
*   **`void zStep(float z)`**: 设置当前的z值步进（防止Z轴重叠导致的深度冲突）
