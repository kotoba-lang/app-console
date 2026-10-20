# app-console（ログ severity）— itonami（dapp）上の分散型設計

状態: 第 1 スライス実装（2026-10-20）。正本はこの文書。
追跡: Linear JUN-128（親 JUN-116）。

## 位置づけ

app-console はログエントリの viewer。**`log/read` のみ・構造的に読み取り
専用**（「変更できるログ viewer は別のもっと危険なプログラム」）。

## このスライス: severity・id 判定コア

| 層 | 実体 | 状態 |
|---|---|---|
| 判定コア guest | `aiueos/console_severity.cljk` — severity-ok（**6 段の順序付き集合**: debug(0)..fault(5)）/ severity-before（**rank 比較** — 文字列順ではない）/ id-composite（**[timestamp, seq] の複合** — タイムスタンプ衝突防止）/ command-admissible（copy-path / export のみ） | **実装済み・実測** |
| 記述子 | `aiueos/console-severity.edn` | **実装済み** |
| parity | `test/app_console/severity_parity_test.cljk` — **実物の model.cljk**（mokuroku+amu+kotoba-sema クラスパス）と一致（levels / level-rank / commands / 複合 id） | **実装済み・実測** |

実測: `amu test aiueos/console_severity.cljk` → **15/15**（jvm-kir / js / wasm）。
parity → 2 tests / 8 assertions / 0 failures。

## 正本の正確な規則（guest はこれに追従 —— 明文の設計意図つき）

- **levels = [:debug :info :notice :warn :error :fault]**（rank 0..5）。
  「文字列ソートは :error を :info より前に置き、severity 順に見えるが
  そうではない」—— 順序はこの並びでしか決まらない。
- **id = [timestamp, seq]**: 「タイムスタンプは衝突する —— 一瞬のログの
  噴出は同じミリ秒を共有する。衝突する id は 2 行を 1 行にし、後から来た方を
  黙って失う」。
- **読み取り専用は構造**: コマンドは copy-path / export のみ。
- seq 列はソート不可（「到着順ソートは Time 列がより正直に言っている」）。

## 実測メモ

- mokuroku item のキーは **`:item/*` 名前空間**（`:item/id` に
  [timestamp, seq]）。`entry->item` は level-rank を attrs の `:severity` に
  計算して載せる —— guest の severity-before と同じ順序表の上に立つ。

## 未実装境界

- ログの検索・フィルタ判定（level / subsystem 絞り込み）。
- page / source の UI 面判定。
- mon（DNA）化、aiueos session 載せ（fleet 共通）。
