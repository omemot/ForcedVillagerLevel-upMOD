# Villager Upgrader Mod

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-green)
![Loader](https://img.shields.io/badge/Loader-NeoForge-orange)

職業を持つ村人を右クリックひとつで即座にレベルアップさせる、シンプルかつ強力なツールを追加するModです。

## 📖 概要

このModは、村人との取引を何度も繰り返す手間を省くために開発されました。専用アイテム「Villager Upgrader」を使用することで、村人のレベル（ティア）を1段階ずつ引き上げ、新しい取引内容を安全に解放します。

## ✨ 特徴

- **ダイレクト・アップグレード**: 右クリックで村人のレベルを +1 します（最大レベル5まで）。
- **インテリジェント・リフレッシュ**: 内部メソッド `updateTrades()` を安全に呼び出すことで、取引リストが消える（無職化する）のを防ぎ、即座に新しい商品を補充します。
- **正確な経験値管理**: バニラの仕様（10, 70, 150, 250 XP）に基づいた正確な経験値設定。
- **エラー防止**: 無職（None）、ニート（Nitwit）、またはすでに達人（Master）の村人には反応しないよう設計されており、アイテムの無駄遣いを防ぎます。

## 🛠 使い方

1. **アイテムの準備**: クリエイティブタブ、または配布パッケージに含まれるレシピから「Villager Upgrader」を入手します。
2. **レベルアップ**: 職業を持っている村人（農民、司書、石工など）に向かってアイテムを使用します。
3. **取引の確認**: 村人のレベルが上がります。新しい取引を表示させるために、一度取引画面を閉じてから再度話しかけてください。

## 💻 技術仕様

- **対応環境**: Minecraft 1.21.1 / NeoForge
- **スレッドセーフ**: `server.execute()` を利用した遅延実行により、サーバーのメインスレッドで安全にデータを書き換えます。
- **アクセス制御**: Javaのリフレクションを利用して `protected` メソッドにアクセスしており、追加の設定なしで動作します。

## 🚀 開発環境でのビルド

1. リポジトリをクローンまたはダウンロード。
2. ターミナルで `./gradlew build` を実行。
3. `build/libs/` 内に生成された JAR ファイルを `mods` フォルダに配置。

---
**Summary (English):**
This NeoForge mod introduces a custom "Villager Upgrader" item that allows players to instantly level up a villager's profession tier while safely refreshing their trade offers using a server-side execution task and precise experience threshold management to ensure stable and consistent gameplay progression.