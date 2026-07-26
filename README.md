<p align="center">
  <img src="./app/src/main/ic_launcher-playstore.png" width="128" alt="BlueApp 应用图标">
</p>

<h1 align="center">BlueApp · 刘小宝</h1>

<p align="center">
  把日记、账目、睡眠与重要日子，安静地留在自己手中。
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Android 7.0+" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Offline First" src="https://img.shields.io/badge/Data-Local%20First-6F97B5">
</p>

BlueApp 是一款使用 Kotlin 与 Jetpack Compose 开发的原生 Android 生活记录应用。它将日记、记账、睡眠和时间记录集中在一个简洁的界面中，核心数据默认保存在本地，无需注册账号，也不依赖网络服务。

> 悟已往之不谏，往日暗沉不可追，来日之路光明灿烂。

## 功能一览

### 📖 日记

- 记录文字、时间、心情与多张照片
- 按年月回顾日记，支持全局日期筛选与正逆序浏览
- 自动生成年度总结：记录天数、连续记录、字数、月度趋势与心情分布
- 在后台分批完成高频词统计与词云分析

### 💰 记账

- 记录收入与支出，支持备注和自定义分类
- 按年、月、日查看账目与收支汇总
- 全局浏览支持年份、月份、类型、分类、关键词和排序筛选
- 年度分析包含分类占比、每月收支、结余、最高支出月份与最大单笔支出

### 🌙 睡眠

- 记录入睡时间与睡前备注
- 通过月度、年度日历直观看到睡眠规律
- 汇总平均入睡时间、最早/最晚入睡时间和熬夜天数
- 可选使用系统“使用情况访问”权限，根据夜间最后一次息屏时间辅助推测入睡时间

### ⏳ 时光

- **岁痕**：设置出生日期，计算已经走过的天数，并用 80 年人生方格展示时间进度
- **去来**：创建倒数日与纪念日，支持事件图片、分类筛选、编辑和删除
- 自动区分未来与已经发生的日子，让等待和回忆都清晰可见

### 📦 数据管理

- 将日记、账目、睡眠、重要日子和图片导出为 ZIP 备份
- 恢复前先校验备份并展示内容摘要
- 支持“合并数据”和“清空后完整恢复”两种模式
- 备份内同时提供结构化 JSON 与便于查看的账目 CSV

备份文件结构与字段定义请参阅 [数据导出格式与适配指南](./EXPORT_DATA_FORMAT.md)。

## 设计特点

- Jetpack Compose + Material 3 构建的全声明式界面
- 低饱和蓝灰配色、大圆角卡片与沉浸式 Edge-to-Edge 布局
- 页面进入、返回和模块切换使用轻量过渡动画
- 首页仅聚合当月统计，数据库与默认数据在后台延迟初始化
- 列表分页、稳定 Key 与生命周期感知的 Flow 收集，兼顾数据量与滚动性能

## 隐私说明

- 核心数据保存在设备本地的 Room 数据库与应用私有目录中
- 应用未申请联网权限，不需要登录或连接云端账号
- “使用情况访问”是可选特殊权限，仅用于辅助估算最后息屏时间；不授权也可以手动记录睡眠
- 导出备份时，由用户自行选择保存位置；恢复数据前会进行格式校验

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 | Kotlin 2.2 |
| UI | Jetpack Compose、Material 3 |
| 架构 | 单 Activity、Repository、ViewModel、单向数据流 |
| 导航 | Navigation Compose |
| 数据 | Room、KSP、Kotlin Coroutines、Flow |
| 图片 | Coil、应用私有文件存储 |
| 构建 | Gradle 9.4、Android Gradle Plugin 9.2 |

## 开始使用

### 环境要求

- Android Studio（建议使用当前稳定版本）
- Android SDK 36
- JDK 17 或更高版本
- Android 7.0（API 24）或更高版本的设备/模拟器

### 获取并运行

```bash
git clone https://github.com/twlSEU/BlueAPP.git
cd BlueAPP
```

使用 Android Studio 打开项目，等待 Gradle 同步完成后，选择设备并运行 `app` 配置即可。

也可以通过命令行构建 Debug APK：

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

构建产物位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```text
app/src/main/java/com/example/blue/
├── core/
│   ├── database/       # Room 数据库、迁移与类型转换
│   ├── navigation/     # 页面路由与转场
│   └── util/           # 金额、日期等通用规则
├── data/
│   ├── backup/         # ZIP 导出、校验与恢复
│   ├── local/          # DAO、Entity 与图片存储
│   └── repository/     # 各业务模块的数据仓库
├── feature/
│   ├── accounting/     # 记账、筛选与年度分析
│   ├── diary/          # 日记、照片与年度总结
│   ├── sleep/          # 睡眠记录与日历统计
│   ├── time/           # 岁痕、倒数日与纪念日
│   ├── backup/         # 备份交互
│   └── home/           # 首页与快捷入口
└── ui/theme/           # 颜色、字体与主题
```

## 参与项目

欢迎通过 [Issues](https://github.com/twlSEU/BlueAPP/issues) 反馈问题或提出建议。如果希望提交代码，请先创建 Issue 说明需求，再提交 Pull Request。

## 许可

本仓库目前未附加开源许可证。未经许可，请勿复制、分发或用于商业用途。
