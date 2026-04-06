# FluxUI
<img width="1367" height="1237" alt="image" src="https://github.com/user-attachments/assets/02586864-4d66-4277-ac47-0aca9bfb9d3b" />

FluxUI 是一个专为 Minecraft 设计的 2D 即时渲染模式界面渲染库。
它基于 Minecraft 的 `TextDisplay` 实体与 JOML 矩阵运算，允许开发者在游戏世界中以低性能开销，声明式地绘制出类似 Dear ImGui 的现代化控制面板。

## 特性

* **即时渲染 (IMGUI)**：每 Tick 的循环中声明式地编写渲染逻辑，底层自动处理实体的生成、更新、缓存与销毁。
* **平台解耦架构**：核心渲染逻辑与 Bukkit/Paper 完全解耦，通过 `UIPool.setPoolFactory` 注入平台实现，可移植到不同服务端或模组端。
* **自动流式排版**：内置 `beginWindow`、`sameLine` 等类似ImGui的排版 API。
* **交互控件**：内置 `Button`、`Checkbox`、`SliderFloat`、`ColorEdit3` 等常用的控件。
* **视线交互**：内置基于 3D 射线检测 (Raycasting) 的交互系统，支持多玩家独立悬停 (Hover) 与点击判定。
* **平滑动画**：支持 Minecraft 原生的 Transformation Interpolation（插值动画）。

## 环境要求

* **Minecraft 版本**: 1.19.4+ (依赖 `TextDisplay` 实体) 或任何带有图形功能的Java程序(需要构造你自己的UIPool实现)
* **依赖库**: `org.joml`

## 快速入门

### 0. 集成FluxUI

只需将 Flux.java 拖入你的项目即可使用，无需配置 Maven/Gradle。

### 1. 注入平台实现 (初始化)
FluxUI 核心不依赖 Bukkit。在插件启动时，你需要先注入实体池的工厂实现（实现 `PoolImpl` 接口来控制 Bukkit 实体的生成，该仓库中的BukkitUIPool）。

```java
@Override
public void onEnable() {
    // 注入 Bukkit 端的 TextDisplay 实体池实现
    Flux.setPoolFactory(location -> new YourBukkitPoolImpl(location));
    
    // ... 其他初始化逻辑
}
```

### 2. 生命周期与事件接管
`Flux` 是基于实例的。你需要为交互的玩家创建 `Flux` 对象，并在主循环中调用 `tick()`。**必须**在玩家退出或插件卸载时调用 `destroy()` 清理实体。

```java
import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyPlugin extends JavaPlugin implements Listener {
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
                        
                        // 1. 更新玩家视线射线 (用于 Hover 检测)
                        Vector3d origin = new Vector3d(player.getEyeLocation().getX(), player.getEyeLocation().getY(), player.getEyeLocation().getZ());
                        Vector3d direction = new Vector3d(player.getLocation().getDirection().getX(), player.getLocation().getDirection().getY(), player.getLocation().getDirection().getZ());
                        flux.updatePlayerRay(player.getUniqueId(), origin, direction);
                        
                        // 2. 触发渲染逻辑
                        flux.tick();                  
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    // 为玩家打开 UI
    public void openUI(Player player) {
        Flux flux = new Flux(player.getUniqueId(), this::renderUI);
        playerUIs.put(player.getUniqueId(), flux);
    }

    // 监听玩家点击事件并传递给 Flux
    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Flux flux = playerUIs.get(event.getPlayer().getUniqueId());
        if (flux != null && event.getAction().isLeftClick()) {
            flux.registerPlayerClick(event.getPlayer().getUniqueId());
        }
    }

    // 玩家退出时销毁 UI，防止实体残留
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Flux flux = playerUIs.remove(event.getPlayer().getUniqueId());
        if (flux != null) flux.destroy();
    }
}
```

### 3. 编写渲染逻辑
借助 Window 和 Layout 系统，你可以像写ImGui一样快速构建 UI。

```java
private boolean isFeatureEnabled = false;
private float speedValue = 1.0f;

private void renderUI(Flux flux) {
    // 定义屏幕在世界中的位置和朝向
    Flux.FluxLocation loc = new Flux.FluxLocation("world", 0, 100, 0);
    Vector3d xAxis = new Vector3d(1, 0, 0);
    Vector3d yAxis = new Vector3d(0, 1, 0);
    Vector3d zAxis = new Vector3d(0, 0, 1);

    // 开启屏幕渲染上下文
    if (flux.screen(loc, xAxis, yAxis, zAxis, "main_screen")) {
        
        // 开启一个自动排版的窗口
        flux.beginWindow("控制面板", 0, 0);

        flux.text("lbl_title", "欢迎使用 FluxUI");

        if (flux.button("btn_heal", "恢复生命值")) {
            System.out.println("玩家点击了治疗按钮！");
        }

        flux.sameLine(); // 下一个控件不换行
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

## 📖 API 参考

### 生命周期与输入系统
* `new Flux(UUID, Consumer<Flux>)`: 实例化 UI 引擎，绑定目标玩家 UUID 与渲染逻辑。
* `tick()`: 推进一帧渲染（需在主循环中每 Tick 调用）。
* `destroy()`: 销毁该实例及其产生的所有实体池。
* `updatePlayerRay(UUID, origin, direction)`: 更新玩家视线起点与方向向量，用于悬停 (Hover) 检测。
* `registerPlayerClick(UUID)`: 注册一次点击事件，触发按钮等控件的交互。

### 屏幕与上下文
* `screen(FluxLocation, xAxis, yAxis, zAxis, id)`: 初始化一个 3D 屏幕的渲染上下文。
* `endScreen()`: 结束当前屏幕的渲染。
* `area(id)` / `endArea()`: 逻辑分组，用于在循环或复杂结构中防止组件 ID 冲突。

### 窗口与排版系统 (Layout)
* `beginWindow(title, startX, startY)`: 开启一个自动向下排版的窗口区域，自带背景和标题栏。
* `endWindow()`: 结束当前窗口，自动计算并包裹内容尺寸。
* `sameLine()`: 标记下一个控件紧跟在当前控件的右侧，而不是换行。

### 交互控件 (Controllers)
* `button(id, text)`: 绘制按钮，被点击时返回 `true`。
* `checkbox(id, label, state)`: 绘制复选框，返回切换后的布尔状态。
* `sliderFloat(id, label, value, min, max)`: 绘制浮点数滑动条，返回拖动后的值。
* `colorEdit3(id, label, FluxColor)`: 绘制颜色预览块。
* `text(id, text)`: 在排版流中绘制普通文本。

### 矩阵变换与动画 (底层)
* `pushMatrix()` / `popMatrix()`: 矩阵栈操作，隔离坐标系变换。
* `translate(x, y, z)` / `scale(x, y, z)`: 平移与缩放。
* `rotateX/Y/Z(angle)`: 绕指定轴旋转（角度制）。
* `skew(angleX, angleY)`: 矩阵切变。
* `interpolation(ticks)`: 设置后续绘制组件的插值动画时长（Tick数）。设为 0 则为瞬间移动。

### 基础图形 (Primitives)
* `rect(id, width, height, FluxColor)`: 绘制纯色矩形。
* `triangle(id, p1, p2, p3, FluxColor)`: 绘制空间三角形。
* `text(id, text, scale, opacity, align)`: 绘制高度自定义的文本。
* *(带有 `Abs` 后缀的方法如 `buttonAbs`, `drawAbsRect` 允许你无视 Layout 系统，直接在指定局部坐标绘制)*

## 范式对比：Dear ImGui vs FluxUI

**Dear ImGui (C++):**
```cpp
ImGui::Begin("Hello, world!");
ImGui::Text("This is some useful text.");
ImGui::Checkbox("Demo Window", &show_demo_window);
ImGui::SliderFloat("float", &f, 0.0f, 1.0f);
if (ImGui::Button("Button")) counter++;
ImGui::SameLine();
ImGui::Text("counter = %d", counter);
ImGui::End();
```

**FluxUI (Java):**
```java
flux.beginWindow("Hello, world!", 0, 0);
flux.text("txt_desc", "This is some useful text.");
show_demo_window = flux.checkbox("chk_demo", "Demo Window", show_demo_window);
f = flux.sliderFloat("sld_f", "float", f, 0.0f, 1.0f);
if (flux.button("btn_counter", "Button")) counter++;
flux.sameLine();
flux.text("txt_cnt", "counter = " + counter);
flux.endWindow();
```

## 注意事项

1. **组件 ID 唯一性**：FluxUI 依赖传入的 `id` 字符串来追踪和复用底层实体。在同一个层级下，请确保每个组件的 ID 是唯一的（例如在循环中使用 `"item_" + index`）。可以使用 `area(id)` 来划分命名空间。
2. **内存泄漏防范**：必须在玩家退出 (`PlayerQuitEvent`) 或插件卸载 (`onDisable`) 时调用 `flux.destroy()`。否则旧的实体将脱离插件控制并残留在世界中。
3. **Z-Fighting (深度冲突)**：FluxUI 底层已实现Z轴的步进机制（自动为同层级组件增加微小的 Z 轴偏移），但仍然建议在外部控制Z轴的偏移，以避免3D模型的渲染错误。

## 鸣谢
+ [TheCymaera](https://github.com/TheCymaera/minecraft-hologram) 提供使用文本展示实体渲染三角形的方案。