# Showdown

Minecraft Forge 1.20.1 服务端模组，提供跨模组生物敌对、队伍管理和战斗数据记录功能。

## 环境要求

- Minecraft 1.20.1
- Forge 47.4.20+
- Java 17

## 构建

```bash
./gradlew build
```

## 功能

### 跨模组敌对

让不同模组的生物互相攻击，实现"模组大战"。支持两种模式：

- **直接指定**：/showdown hostile <mod1> <mod2>
- **队伍模式**：将多个模组分组到不同队伍，队伍之间互为敌对

### 队伍系统

将模组分组管理，同一队伍内的生物互不攻击，不同队伍的生物互相敌对。

### 战斗数据记录

开启记录后自动统计各队伍的击杀数和死亡数，关闭时输出统计报告。

### 游戏控制开关

| 命令 | 功能 |
|------|------|
| /showdown nodrops | 禁止物品掉落 |
| /showdown noexps | 禁止经验球掉落 |
| /showdown peaceful | 和平模式，怪物不攻击玩家 |
| /showdown godmode | 玩家无敌，免疫所有伤害和药水效果 |

## 命令一览

所有命令需要权限等级 2（默认为管理员）。

| 命令 | 说明 |
|------|------|
| /showdown hostile <mod1> <mod2> | 让两个模组的生物互相敌对 |
| /showdown cancel | 取消所有敌对状态 |
| /showdown nodrops | 切换物品掉落 |
| /showdown noexps | 切换经验球掉落 |
| /showdown peaceful | 切换和平模式 |
| /showdown godmode | 切换无敌模式 |
| /showdown record | 开始/停止战斗数据记录（再次执行时显示统计） |
| /showdown team create <name> | 创建队伍 |
| /showdown team delete <name> | 删除队伍 |
| /showdown team add <team> <mod> | 将模组加入队伍 |
| /showdown team remove <mod> | 将模组从队伍中移除 |
| /showdown team list | 列出所有队伍及其成员 |
| /showdown team apply | 应用队伍敌对设置 |
| /showdown team clear | 清除所有敌对状态 |

## 使用示例

**快速开始 — 两个模组对战：**

```mcfunction
/showdown hostile iceandfire twilightforest
/showdown record
```

**队伍模式 — 多模组分组对战：**

```mcfunction
/showdown team create heroes
/showdown team create villains
/showdown team add heroes minecraft
/showdown team add villains iceandfire
/showdown team add villains twilightforest
/showdown team apply
/showdown record
```

战斗结束后再次执行 /showdown record 查看统计：

```text
§6=== 战斗统计 (记录时长: 120秒) ===
§eheroes§r: 击杀 §a15§r / 死亡 §c8
§evillains§r: 击杀 §a8§r / 死亡 §c15
§a已停止数据记录
```

## 数据持久化

队伍配置保存在 config/showdown-teams.json，服务器重启后自动加载。

## 项目结构

```text
src/main/java/alku/showdown/
├── Showdown.java           # 主类，事件监听与功能开关
├── ModCommand.java         # /showdown 命令树注册
├── ModFeudManager.java     # 敌对关系管理与实体扫描缓存
├── ModFeudTargetGoal.java  # 自定义 AI 目标（仅攻击敌对队伍）
├── ModTeamManager.java     # 队伍数据持久化（JSON）
├── FeudStatistics.java     # 战斗数据统计
└── Config.java             # Forge 配置（模板）
```

## 许可证

All Rights Reserved