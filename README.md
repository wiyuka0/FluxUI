# FluxUI

FluxUI 是一个专为 Minecraft (Bukkit/Paper) 设计的 2D 即时渲染模式 (Immediate Mode GUI, IMGUI) 界面渲染库。
它基于 Minecraft 的 `TextDisplay` 实体与 JOML 矩阵运算，允许开发者在游戏世界中以极低的性能开销绘制出类似 Dear ImGui 的现代化控制面板。

## 特性

* **即时渲染**：每 Tick 的循环中声明式地编写渲染逻辑，底层自动处理实体的生成、更新与销毁。
* **面向实例设计**：为每个玩家或每个交互面板创建独立的 `Flux` 实例。
* **排版系统**：`beginWindow` 和 `sameLine` 等排版 API实现自动流式布局。
* **交互控件**：内置 `Button`、`Checkbox`、`SliderFloat`、`ColorEdit3` 等控件。
* **实体池化**：自动缓存并复用 `TextDisplay` 实体。
* **视线与点击交互**：内置基于射线检测 (Raycasting) 的交互系统，可实现点击 拖动等。
* **插值动画**：支持 Minecraft 原生的 Transformation Interpolation。

## 环境要求

* **Minecraft 版本**: 1.19.4+ (依赖 `TextDisplay` 实体)
* **服务端核心**: Paper / Purpur / Folia (必须包含 `org.joml` 库)

## 快速入门

### 1. 初始化与生命周期管理

`Flux` 是基于实例的。你需要为玩家创建一个 `Flux` 对象，并在主循环中调用 `tick()`。**必须**在玩家退出或插件卸载时调用 `destroy()` 清理实体。

```java
import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyPlugin extends JavaPlugin implements Listener {
    // 存储每个玩家的 UI 实例
    private final Map<UUID, Flux> playerUIs = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // 启动全局 UI 刷新任务
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Flux> entry : playerUIs.entrySet()) {
                    Player player = getServer().getPlayer(entry.getKey());
                    if (player != null) {
                        Flux flux = entry.getValue();
                        flux.updatePlayerRay(player); // 更新玩家视线射线
                        flux.tick();                  // 触发渲染逻辑
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    // 为玩家打开 UI
    public void openUI(Player player) {
        Flux flux = new Flux(player, this::renderUI);
        playerUIs.put(player.getUniqueId(), flux);
    }

    // 监听玩家点击事件并传递给 Flux
    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Flux flux = playerUIs.get(event.getPlayer().getUniqueId());
        if (flux != null && event.getAction().isLeftClick()) {
            flux.registerPlayerClick(event.getPlayer());
        }
    }

    // 玩家退出时销毁 UI，防止实体残留
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Flux flux = playerUIs.remove(event.getPlayer().getUniqueId());
        if (flux != null) flux.destroy();
    }

    @Override
    public void onDisable() {
        playerUIs.values().forEach(Flux::destroy);
        playerUIs.clear();
    }
}
```

### 2. 编写渲染逻辑 (使用排版系统)

借助全新的 Window 和 Layout 系统，你可以像写网页一样快速构建 UI，而不需要手动计算每个元素的 X/Y 坐标。

```java
import org.bukkit.Color;
import org.bukkit.Location;
import org.joml.Vector3f;

private boolean isFeatureEnabled = false;
private float speedValue = 1.0f;

private void renderUI(Flux flux) {
    Location loc = new Location(getServer().getWorld("world"), 0, 100, 0);
    Vector3f xAxis = new Vector3f(1, 0, 0);
    Vector3f yAxis = new Vector3f(0, 1, 0);
    Vector3f zAxis = new Vector3f(0, 0, 1);

    // 开启屏幕渲染上下文
    if (flux.screen(loc, xAxis, yAxis, zAxis, "main_screen")) {
        
        flux.beginWindow("控制面板", 0, 0);

        flux.text("lbl_title", "欢迎使用 FluxUI");

        if (flux.button("btn_heal", "恢复生命值")) {
            System.out.println("玩家点击了治疗按钮！");
        }

        flux.sameLine();
        if (flux.button("btn_feed", "恢复饥饿值")) {
            System.out.println("玩家点击了喂食按钮！");
        }

        isFeatureEnabled = flux.checkbox("chk_fly", "允许飞行", isFeatureEnabled);

        speedValue = flux.sliderFloat("sld_speed", "移动速度", speedValue, 0.1f, 2.0f);

        flux.endWindow();
        
        flux.endScreen();
    }
}
```

## API 参考

### 生命周期与输入系统
* `new Flux(Player, Consumer<Flux>)`: 实例化 UI 引擎并绑定渲染逻辑。
* `tick()`: 推进一帧渲染（需在 BukkitRunnable 中每 Tick 调用）。
* `destroy()`: 销毁该实例及其产生的所有实体。
* `updatePlayerRay(Player)`: 更新玩家视线，用于悬停 (Hover) 检测。
* `registerPlayerClick(Player)`: 注册一次点击事件，触发按钮等控件的交互。

### 屏幕与上下文
* `screen(Location, xAxis, yAxis, zAxis, id)`: 初始化一个 3D 屏幕的渲染上下文。
* `endScreen()`: 结束当前屏幕的渲染。
* `area(id)` / `endArea()`: 逻辑分组，用于在循环或复杂结构中防止组件 ID 冲突。

### 窗口与排版系统 (Layout)
* `beginWindow(title, startX, startY)`: 开启一个自动向下排版的窗口区域。
* `endWindow()`: 结束当前窗口。
* `sameLine()`: 标记下一个控件紧跟在当前控件的右侧，而不是换行。

### 交互控件 (Controllers)
* `button(id, text)`: 绘制按钮，被点击时返回 `true`。
* `checkbox(id, label, state)`: 绘制复选框，返回切换后的布尔状态。
* `sliderFloat(id, label, value, min, max)`: 绘制浮点数滑动条，返回拖动后的值。
* `colorEdit3(id, label, color)`: 绘制颜色选择器。
* `text(id, text)`: 在排版流中绘制普通文本。

### 矩阵变换与动画 (底层)
* `pushMatrix()` / `popMatrix()`: 矩阵栈操作，隔离坐标系变换。
* `translate(x, y, z)` / `scale(x, y, z)`: 平移与缩放。
* `rotateX/Y/Z(angle)`: 绕指定轴旋转。
* `interpolation(ticks)`: 设置后续绘制组件的插值动画时长（Tick数）。设为 0 则为瞬间移动。

### 基础图形 (Primitives)
* `rect(id, width, height, color)`: 绘制纯色矩形。
* `triangle(id, p1, p2, p3, color)`: 绘制空间三角形。
* `text(id, text, scale, opacity, align)`: 绘制高度自定义的文本。
* *(带有 `Abs` 后缀的方法如 `buttonAbs`, `drawAbsRect` 允许你无视 Layout 系统，直接在指定坐标绘制)*

## 注意事项

1. **组件 ID 唯一性**：FluxUI 依赖传入的 `id` 字符串来追踪和复用底层实体。在同一个屏幕层级下，请确保每个组件的 ID 是唯一的（例如在循环中使用 `"item_" + index`）。
2. **内存泄漏防范**：必须在玩家退出 (`PlayerQuitEvent`) 或插件卸载 (`onDisable`) 时调用 `flux.destroy()`。否则旧的实体将脱离插件控制并永久残留在世界中。
3. **Z-Fighting (深度冲突)**：虽然系统内部实现了微调来自动处理同一平面上的深度冲突，但在手动使用矩阵嵌套结构时，仍建议使用 `flux.translate(0, 0, 0.01f)` 来明确图层的前后关系。