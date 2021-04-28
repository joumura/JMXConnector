   JMX情報取得バッチ

2016/10/27

[使い方]
- JMXConnector-0.0.1.jar と同じディレクトリに、実行時利用するJREと同じ版のJDKにある tools.jar を配置する。シンボリックリンク可。
- JMXConnector.propertites に取得したいオブジェクト名と属性名を、半角スペースで区切って記述する。1行に1件。
- jar実行時の引数で、対象のJavaVMを限定する。文字列は部分一致。未指定でも可。必ずしも1VMに限定する必要はない。
-- 例) java -jar JMXConnector-0.0.1.jar kt12
- 実行すると、JMXConnector.propertitesに記した項目の値が、その記述順に第２列からCSV形式で標準出力される。先頭列は対象VMのPID。
-- 複数VMを対象とした場合には、複数行出力される。

- 対象VMと同じOS内、同じユーザーアカウントでjarを実行する必要がある。(JMX local connect)
