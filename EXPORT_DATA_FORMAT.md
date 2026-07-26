# BlueApp 导出备份数据格式与适配指南

本文档描述 BlueApp 当前导出功能生成的备份文件格式，供其他程序将自己的数据转换为 BlueApp 可恢复的数据。

> 当前备份格式版本：`4`  
> 备份文件类型：ZIP（MIME 类型 `application/zip`）  
> 默认文件名：`life_record_backup_yyyy-MM-dd_HHmmss.zip`  
> 文本编码：UTF-8

## 1. 最重要的适配结论

如果要让 BlueApp 恢复你生成的数据，请创建一个 ZIP 文件，并至少在 ZIP 根目录放入以下 6 个文件：

```text
manifest.json
diaries.json
accounts.json
categories.json
sleeps.json
time.json
```

推荐同时生成 `accounts.csv`，以保持和 BlueApp 自身导出的文件一致；但恢复功能不会读取这个 CSV。

完整结构如下：

```text
life_record_backup_2026-07-18_223000.zip
├── manifest.json          # 备份版本、导出时间和数据量
├── diaries.json           # 日记及其图片引用
├── accounts.json          # 账目
├── categories.json        # 账目分类
├── sleeps.json            # 睡眠记录
├── time.json              # 年龄、生日和时光事件
├── accounts.csv           # 账目的人类可读副本，不参与恢复
├── images/                # 可选：日记图片
│   ├── diary_photo_1.jpg
│   └── diary_photo_2.png
└── time_images/           # 可选：时光事件图片
    └── event_cover.jpg
```

适配时应使用 `formatVersion: 4`。JSON 顶层类型、枚举值、日期格式、图片路径和 ID 关联必须符合下文约定。

## 2. 通用数据约定

### 2.1 类型和格式

| 表示 | 格式 | 示例 | 说明 |
|---|---|---|---|
| ID | JSON 字符串 | `"550e8400-e29b-41d4-a716-446655440000"` | 建议使用非空 UUID，并满足相应唯一约束 |
| 日期 | ISO-8601 本地日期 | `"2026-07-18"` | 必须能被 `LocalDate.parse` 解析 |
| 时间 | ISO-8601 本地时间 | `"08:30"`、`"08:30:15"` | 必须能被 `LocalTime.parse` 解析；没有时区 |
| 日期时间 | ISO-8601 本地日期时间 | `"2026-07-18T23:10"` | 必须能被 `LocalDateTime.parse` 解析；没有时区 |
| 时间戳 | JSON 整数 | `1784395800000` | Unix Epoch 毫秒，类型为 64 位整数 |
| 可空值 | JSON `null` | `"note": null` | 不要写成字符串 `"null"` |
| 金额 | JSON 整数 | `1299` | 单位为分，`1299` 表示 `12.99` 元 |

日期、时间和日期时间中不要附加 `Z`、`+08:00` 等时区信息。

### 2.2 ZIP 和大小限制

| 项目 | 上限 |
|---|---:|
| 整个 ZIP | 2 GiB |
| 每个 JSON/CSV 文本文件 | 64 MiB |
| 每张图片 | 100 MiB |

ZIP 内文件名使用 `/` 作为目录分隔符。图片引用只允许下面两种精确形式：

- 日记图片：`images/<文件名>`
- 时光图片：`time_images/<文件名>`

`<文件名>` 必须只是 basename，不能包含额外目录、`..` 或绝对路径。例如 `images/a.jpg` 合法，`images/2026/a.jpg` 和 `../a.jpg` 不合法。

### 2.3 必填与默认值

下文的“必填”依据当前恢复代码，而不是仅依据 BlueApp 自身的导出结果。缺少必填字段、字段类型错误、日期无法解析或枚举值不匹配，都会导致恢复失败。

部分时间戳和布尔字段在恢复时有默认值，但仍建议全部显式提供，以免合并数据时出现非预期覆盖。

## 3. `manifest.json`

顶层是一个 JSON 对象。

### 3.1 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `formatVersion` | integer | 是 | 自定义适配文件应固定写 `4`；当前应用接受 `1..4` |
| `appVersion` | string | 否 | 生成备份的应用版本，仅作说明，恢复时不读取 |
| `exportedAt` | long | 是 | 导出时的 Unix Epoch 毫秒 |
| `diaryCount` | integer | 否 | 日记数量；缺失时显示为 `0` |
| `accountCount` | integer | 否 | 账目数量；缺失时显示为 `0` |
| `sleepCount` | integer | 否 | 睡眠记录数量；缺失时显示为 `0` |
| `timeEventCount` | integer | 否 | 时光事件数量；缺失时显示为 `0` |
| `imageCount` | integer | 否 | 实际打包的日记图片与时光图片总数；缺失时显示为 `0` |

各个 count 字段用于恢复前的摘要展示，当前不会与 JSON 中的真实数量交叉校验，但应填写真实值。

### 3.2 示例

```json
{
  "formatVersion": 4,
  "appVersion": "external-adapter-1.0",
  "exportedAt": 1784395800000,
  "diaryCount": 1,
  "accountCount": 1,
  "sleepCount": 1,
  "timeEventCount": 1,
  "imageCount": 2
}
```

## 4. `categories.json`：账目分类

顶层是分类对象数组。恢复时分类先于账目写入数据库，因此 `accounts.json` 中引用的分类必须在这里存在。

### 4.1 字段

| 字段 | 类型 | 必填 | 允许值/默认值 | 说明 |
|---|---|---:|---|---|
| `id` | string | 是 | 唯一 | 分类主键，被账目的 `categoryId` 引用 |
| `name` | string | 是 | — | 分类名称 |
| `type` | string | 是 | `INCOME`、`EXPENSE` | 收入或支出分类；区分大小写 |
| `isDefault` | boolean | 否 | 默认 `false` | 是否为内置默认分类 |
| `isActive` | boolean | 否 | 默认 `true` | 是否仍可用 |
| `createdAt` | long | 否 | 默认 `0` | 创建时间，Epoch 毫秒 |
| `updatedAt` | long | 否 | 默认 `0` | 更新时间，Epoch 毫秒 |

数据库要求 `(type, name)` 组合唯一。不要创建两个类型和名称都相同的分类。

### 4.2 示例

```json
[
  {
    "id": "cat-food",
    "name": "餐饮",
    "type": "EXPENSE",
    "isDefault": false,
    "isActive": true,
    "createdAt": 1784395800000,
    "updatedAt": 1784395800000
  },
  {
    "id": "cat-salary",
    "name": "工资",
    "type": "INCOME",
    "isDefault": false,
    "isActive": true,
    "createdAt": 1784395800000,
    "updatedAt": 1784395800000
  }
]
```

## 5. `accounts.json`：账目

顶层是账目对象数组。

### 5.1 字段

| 字段 | 类型 | 必填 | 允许值/默认值 | 说明 |
|---|---|---:|---|---|
| `id` | string | 是 | 唯一 | 账目主键 |
| `date` | string | 是 | `yyyy-MM-dd` | 账目日期 |
| `time` | string | 是 | ISO 本地时间 | 账目时间 |
| `type` | string | 是 | `INCOME`、`EXPENSE` | 收入或支出；区分大小写 |
| `amountInCents` | long | 是 | 整数分 | 金额，建议大于 `0` |
| `name` | string | 是 | — | 账目名称 |
| `categoryId` | string | 是 | 已存在的分类 ID | 外键，必须指向 `categories.json` 中的分类 |
| `note` | string 或 null | 否 | 默认 null | 备注；空字符串也会按无备注处理 |
| `createdAt` | long | 否 | 默认 `0` | 创建时间，Epoch 毫秒 |
| `updatedAt` | long | 否 | 默认 `0` | 更新时间，Epoch 毫秒 |

建议让账目的 `type` 与被引用分类的 `type` 一致。数据库外键只检查分类 ID 是否存在，但类型不一致会造成业务语义错误。

### 5.2 示例

```json
[
  {
    "id": "account-001",
    "date": "2026-07-18",
    "time": "12:30",
    "type": "EXPENSE",
    "amountInCents": 2590,
    "name": "午餐",
    "categoryId": "cat-food",
    "note": "和朋友一起",
    "createdAt": 1784368200000,
    "updatedAt": 1784368200000
  }
]
```

## 6. `diaries.json`：日记和日记图片

顶层是日记对象数组。图片元数据嵌套在对应日记的 `images` 数组中，不存在独立的图片 JSON 文件。

### 6.1 日记字段

| 字段 | 类型 | 必填 | 允许值/默认值 | 说明 |
|---|---|---:|---|---|
| `id` | string | 是 | 唯一 | 日记主键；图片会自动关联到该 ID |
| `date` | string | 是 | `yyyy-MM-dd` | 日记日期 |
| `time` | string | 是 | ISO 本地时间 | 日记时间 |
| `content` | string | 否 | 默认空字符串 | 日记正文 |
| `mood` | integer 或 null | 否 | `1..6` | 其他整数、缺失或 null 都会转为无心情 |
| `createdAt` | long | 否 | 默认 `0` | 创建时间，Epoch 毫秒 |
| `updatedAt` | long | 否 | 默认 `0` | 更新时间，Epoch 毫秒 |
| `images` | array | 否 | 默认 `[]` | 日记图片元数据数组 |

### 6.2 `images[]` 字段

| 字段 | 类型 | 必填 | 允许值/默认值 | 说明 |
|---|---|---:|---|---|
| `id` | string | 否 | 默认生成 UUID | 图片记录 ID，建议显式提供且全局唯一 |
| `path` | string | 是 | `images/<文件名>` | ZIP 内图片的精确路径 |
| `sortOrder` | integer | 否 | 默认使用数组下标 | 图片显示顺序，通常从 `0` 开始 |
| `createdAt` | long | 否 | 默认恢复时的当前时间 | 创建时间，Epoch 毫秒 |

如果 JSON 引用了格式正确但 ZIP 中不存在的图片文件，当前恢复逻辑会跳过该图片，不会使整个恢复失败。

恢复时会为图片生成新的本地文件名，因此 JSON 中的 `path` 只是备份包内路径，不是恢复后的设备路径。

### 6.3 示例

```json
[
  {
    "id": "diary-001",
    "date": "2026-07-18",
    "time": "21:45",
    "content": "今天完成了数据迁移。",
    "mood": 5,
    "createdAt": 1784401500000,
    "updatedAt": 1784401500000,
    "images": [
      {
        "id": "diary-image-001",
        "path": "images/diary_photo_1.jpg",
        "sortOrder": 0,
        "createdAt": 1784401500000
      }
    ]
  }
]
```

## 7. `sleeps.json`：睡眠记录

顶层是睡眠记录对象数组。

### 7.1 字段

| 字段 | 类型 | 必填 | 允许值/默认值 | 说明 |
|---|---|---:|---|---|
| `id` | string | 是 | 在本文件内唯一 | 睡眠记录主键 |
| `recordDate` | string | 是 | `yyyy-MM-dd`，在本文件内唯一 | 记录归属日期；数据库也只允许每天一条 |
| `sleepDateTime` | string | 是 | ISO 本地日期时间 | 入睡时间 |
| `wakeDateTime` | string 或 null | 否 | 默认 null | 起床时间；非 null 时必须为 ISO 本地日期时间，且晚于入睡时间 |
| `source` | string | 是 | 见下表 | 数据来源；区分大小写 |
| `isEstimated` | boolean | 否 | 默认 `false` | 是否为估算值 |
| `note` | string 或 null | 否 | 默认 null | 备注 |
| `createdAt` | long | 否 | 默认 `0` | 创建时间，Epoch 毫秒 |
| `updatedAt` | long | 否 | 默认 `0` | 更新时间，Epoch 毫秒；合并时用于判断新旧 |

`source` 枚举：

| 值 | 含义 |
|---|---|
| `MANUAL` | 手动录入 |
| `SYSTEM_ESTIMATE` | 系统估算 |
| `MANUAL_CONFIRMED` | 用户确认过的记录 |

### 7.2 示例

```json
[
  {
    "id": "sleep-2026-07-18",
    "recordDate": "2026-07-18",
    "sleepDateTime": "2026-07-17T23:30",
    "wakeDateTime": "2026-07-18T07:20",
    "source": "MANUAL",
    "isEstimated": false,
    "note": null,
    "createdAt": 1784311800000,
    "updatedAt": 1784340000000
  }
]
```

## 8. `time.json`：个人时间信息和时光事件

顶层是一个 JSON 对象，而不是数组。

### 8.1 顶层字段

| 字段 | 类型 | 必填 | 允许值/默认值 | 说明 |
|---|---|---:|---|---|
| `age` | integer 或 null | 否 | `0..80` | 年龄 |
| `birthday` | string 或 null | 否 | 不晚于当前日期的 `yyyy-MM-dd` | 生日 |
| `events` | array | 否 | 默认 `[]` | 时光事件数组 |

当 `age` 和 `birthday` 都为 null 或缺失时，不创建个人时间信息。只提供 `birthday` 时，恢复后的 `age` 会使用 `0`；因此建议根据需要同时提供两者。

### 8.2 `events[]` 字段

| 字段 | 类型 | 必填 | 允许值/默认值 | 说明 |
|---|---|---:|---|---|
| `id` | string | 是 | 在本文件内唯一 | 事件主键 |
| `title` | string | 是 | 去除首尾空白后不能为空 | 事件标题 |
| `date` | string | 是 | `yyyy-MM-dd` | 目标日期或纪念日期 |
| `type` | string | 是 | `COUNTDOWN`、`ANNIVERSARY` | 事件类型；区分大小写 |
| `imagePath` | string 或 null | 否 | `time_images/<文件名>` | ZIP 内封面图片路径 |
| `createdAt` | long | 否 | 默认 `0` | 创建时间，Epoch 毫秒 |
| `updatedAt` | long | 否 | 默认 `0` | 更新时间，Epoch 毫秒；合并时用于判断新旧 |

事件类型含义：

- `COUNTDOWN`：倒数日/未来目标。
- `ANNIVERSARY`：纪念日。

如果 `imagePath` 指向的 ZIP 条目不存在，事件仍会恢复，但不带图片。

### 8.3 示例

```json
{
  "age": 28,
  "birthday": "1998-03-12",
  "events": [
    {
      "id": "time-event-001",
      "title": "旅行出发",
      "date": "2026-10-01",
      "type": "COUNTDOWN",
      "imagePath": "time_images/event_cover.jpg",
      "createdAt": 1784395800000,
      "updatedAt": 1784395800000
    }
  ]
}
```

## 9. `accounts.csv`：只读辅助文件

该文件是账目的表格化副本，方便用户用表格软件查看。BlueApp 恢复时完全不读取它，不能用它替代 `accounts.json` 或 `categories.json`。

- 编码：UTF-8
- 首行表头：`日期,时间,类型,金额,名称,分类,备注`
- 每个数据字段都使用双引号包围。
- 字段中的双引号使用两个双引号转义。
- 类型显示为中文 `收入` 或 `支出`。
- 金额单位为元，固定两位小数，并可能带本地化千位分隔符。

示例：

```csv
日期,时间,类型,金额,名称,分类,备注
"2026-07-18","12:30","支出","25.90","午餐","餐饮","和朋友一起"
```

不要从该 CSV 反推精确分值；生成可恢复备份时，以 `accounts.json.amountInCents` 为准。

## 10. 文件之间的关联关系

```text
categories.json: category.id
          ▲
          │ accounts[].categoryId（必须存在）
          │
accounts.json

diaries.json: diary.id
          └── diary.images[]（自动归属于所在日记）
                         └── path ──> ZIP/images/<文件名>

time.json: events[]
          └── imagePath ──> ZIP/time_images/<文件名>
```

主要唯一性要求：

- 所有数据库实体的 `id` 应各自在对应实体类型中唯一。
- 分类的 `(type, name)` 组合必须唯一。
- 睡眠记录的 `recordDate` 必须唯一，即一天最多一条。
- `accounts[].categoryId` 必须引用存在的分类。
- 同一 ZIP 路径只能对应一个文件。

## 11. 最小可导入备份

如果某类数据为空，仍需为版本 4 创建对应文件。最小数据内容如下。

`manifest.json`：

```json
{
  "formatVersion": 4,
  "appVersion": "external-adapter-1.0",
  "exportedAt": 1784395800000,
  "diaryCount": 0,
  "accountCount": 0,
  "sleepCount": 0,
  "timeEventCount": 0,
  "imageCount": 0
}
```

以下文件内容均为一个空数组：

```text
diaries.json    -> []
accounts.json   -> []
categories.json -> []
sleeps.json     -> []
```

`time.json`：

```json
{
  "age": null,
  "birthday": null,
  "events": []
}
```

`accounts.csv` 可以只包含表头，也可以省略。

## 12. 合并恢复与完整恢复的行为

用户选择恢复时有两种模式，这会影响外部数据的 ID 和时间戳设计。

### 合并数据

- 分类、账目、日记按 ID 执行 upsert；相同 ID 的记录会被导入记录覆盖。
- 相同日记 ID 的原图片列表会被导入日记的图片列表替换。
- 睡眠按 `recordDate` 合并：没有同日记录则新增；存在同日记录时，仅当导入记录的 `updatedAt` 不早于现有记录才覆盖。
- 新睡眠记录如与其他日期的记录发生 ID 冲突，应用会为它生成新 UUID。
- 时光事件按 ID 合并；仅当导入事件的 `updatedAt` 不早于现有事件时才覆盖。
- 个人年龄/生日在 `time.json` 能形成 profile 时直接写入。

因此，从其他系统持续同步数据时，应该为同一条业务记录使用稳定 ID，并正确维护递增的 `updatedAt`。

### 清空后完整恢复

应用会先清空当前账目、分类、日记、睡眠和时光数据，再写入备份内容；原有图片也会在数据库提交成功后清理。

## 13. 推荐的外部数据映射流程

1. 为每类数据建立稳定 ID；推荐 UUID，避免多次导入产生重复记录。
2. 先转换分类，建立“外部分类 ID → BlueApp 分类 ID”的映射。
3. 再转换账目，把每条账目的 `categoryId` 替换为上一步生成的分类 ID。
4. 将金额换算为整数分，避免浮点误差。例如用十进制定点数计算 `12.34 × 100 = 1234`。
5. 将日期、时间、日期时间统一转换为无时区的 ISO-8601 字符串。
6. 生成日记、睡眠和时光事件 JSON，并校验必填枚举值。
7. 将图片以安全的唯一文件名复制到 `images/` 或 `time_images/`，再填写完全一致的 JSON 路径。
8. 根据最终数据和实际打包成功的图片计算 manifest 中的 count。
9. 所有 JSON 用 UTF-8 写入 ZIP 根目录；不要在 ZIP 外再套一层同名文件夹。
10. 在正式恢复前，先用少量数据测试“合并数据”，核对分类、金额、时间和图片。

## 14. 生成前检查清单

- [ ] ZIP 根目录存在 6 个必需 JSON 文件。
- [ ] `manifest.json.formatVersion` 等于 `4`。
- [ ] 所有 JSON 都是合法 UTF-8，顶层对象/数组类型正确。
- [ ] 枚举严格使用大写英文值，且没有多余空格。
- [ ] 金额是整数分，而不是小数元。
- [ ] 日期和时间没有时区后缀。
- [ ] 所有账目的 `categoryId` 都能在分类中找到。
- [ ] 分类 `(type, name)` 没有重复。
- [ ] 睡眠记录的 ID 和 `recordDate` 没有重复。
- [ ] 起床时间为空，或严格晚于入睡时间。
- [ ] 时光事件标题去除首尾空格后仍非空，事件 ID 没有重复。
- [ ] 年龄在 `0..80`，生日不晚于恢复当天。
- [ ] 图片路径只有 `images/<文件名>` 或 `time_images/<文件名>` 两段。
- [ ] JSON 中引用的每张图片实际存在于 ZIP 中且不超过 100 MiB。
- [ ] 单个文本文件不超过 64 MiB，整个备份不超过 2 GiB。
- [ ] count 字段与实际数据量一致。

## 15. 兼容性说明

当前应用接受备份格式版本 `1` 到 `4`：

- 版本 1：基础日记、账目和分类。
- 版本 2：开始读取 `sleeps.json`。
- 版本 3：开始读取 `time.json`。
- 版本 4：当前导出版本，包含当前完整字段（包括生日）。

新适配器不应生成旧版本。旧版本规则只用于理解历史备份；使用版本 4 可以避免睡眠或时光数据被忽略。
