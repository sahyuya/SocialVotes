# SocialVotes機能まとめ
## SV看板の作り方
看板に下記記入
```
１行目　vote
２行目　名前
```
## コマンド一覧
```
正式名称　 /socialvotes
エイリアス /sv
```
### `/sv setgroup <name>`
- 投票グループを作成
### `/sv add <name>`
- SV看板のグループ追加状態になり、クリックで追加
### `/sv remove`
- SV看板のグループ離脱状態になり、クリックで追放
### `/sv list (<name>)`
- グループ一覧表示
- グループ名追記の場合、SV看板一覧表示
### `/sv allclear <name>`
- グループと属するSV看板を一括消去
### `/sv delhere`
- 実行者の足元のSV看板を消去
### `/sv startvote <name>`
- グループを強制的に投票可能にする(開始時刻上書き)
### `/sv stopvote <name>`
- グループを強制的に投票不可能にする(終了時刻上書き)
### `/svtp <id>`
- 指定IDのSV看板へテレポート
### `/svupdate`
- 同じ記録のSV看板があった際、クリックした看板を正式なSV看板にする(元看板が消えててもOK)
