# 手机壳候选风格

目标是替换当前粗黑圆角矩形，默认导出接近用户给的图二：浅色背景、窄边框安卓打孔屏、真实金属边、柔和阴影。

## 候选说明

| 编号 | 风格 | 参考来源 | 授权风险 | 落地方式 |
| --- | --- | --- | --- | --- |
| A | Pixel 9 Pro Porcelain | Figma 社区 Pixel mockup / Google Pixel 视觉语言 | 中 | 程序化复刻，不直接嵌入素材 |
| B | Pixel 9 Pro Obsidian | Figma 社区 Pixel mockup / Google Pixel 视觉语言 | 中 | 程序化复刻 |
| C | Galaxy S Ultra Titanium | Samsung 官方渲染风格 / mockup generator | 中 | 程序化复刻 |
| D | Galaxy Z Fold Front | Samsung 折叠屏正面视觉 | 中 | 程序化复刻 |
| E | Xiaomi Ultra Silver | 国内安卓旗舰银色金属边 | 低 | 程序化复刻 |
| F | OnePlus Emerald | 一加绿色金属边视觉 | 低 | 程序化复刻 |
| G | OPPO/Vivo Pearl | OPPO/vivo 浅色旗舰边框 | 低 | 程序化复刻 |
| H | Honor Magic Green | 荣耀青绿色窄边框 | 低 | 程序化复刻 |
| I | Huawei Mate Green | 华为 Mate 青绿色边框 | 低 | 程序化复刻 |
| J | Generic Premium Dark | 通用高端黑色安卓框 | 低 | 程序化复刻 |

## 推荐默认

默认用 **A：Pixel 9 Pro Porcelain** 或 **G：OPPO/Vivo Pearl**。这两种最接近图二：浅色金属边、窄黑内边、打孔屏、适合浅米绿色背景。

## 待确认

请从 `phone-frame-candidates.svg` 里选 2-4 个编号。确认后我会接入 App，并增加：

- 默认 `3:4` 导出比例。
- 浅米绿色默认背景。
- 自定义比例：`1:1 / 3:4 / 4:5 / 9:16 / 原图`。
- 背景色预设 + HEX 输入。
