# clione-sql
Automatically exported from code.google.com/p/clione-sql


Introduction
clione-sqlの使用方法です。
基本編、応用編に分かれています。
開発を始めるには、まずは基本編に目を通しておけば充分でしょう。
基本編の内容では対応できない問題が発生した場合には、応用編を読んで対応してください。

基本編
1. 「2WaySQL」とは
まずは簡単なサンプルを使用して、2WaySQLの概念を説明します。
下記のようなsqlファイルを用意します。

[Sample.sql]

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 = /* param1 */100

    AND FIELD2 = /* param2 */'AAA'

これを適切なパス(後述)に置き、Javaで次のように実装します。


import static tetz42.clione.SQLManager.*;

           :

    public List<Entity> findTable1ByParam1AndParam2(int param1, String param2) {

        return sqlManager().useFile(getClass(), "Sample.sql")

                .findAll( Entity.class, params("param1", param1).$("param2", param2) );

    }

すると、上記Sample.sqlは下記のように変換され、適切にparam1, 2の値がbindされて実行されます。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 = ?

    AND FIELD2 = ?


Sample.sqlのテンプレート内で使用されている「/ param1 /」はANSIで定められたSQL文のコメントであるため、Sample1.sqlはそのまま通常のSQLとして実行することができます。このように、テンプレートでありながらも通常のSQL文としても扱える仕組みのことを、2WaySQL と言います。
この仕組みによりclione-sqlでは、テンプレートとなるSQL文を実際に実行したり解析したりしながら開発＆リファクタリングすることが可能となります。

※上の説明を読んだだけではピンとこないかも知れませんが、実際にこの2WaySQLという仕組みを体験すると、その便利さに驚くと思います。
いつでも実行できるSQLファイルをテンプレートを使うと、リファクタリングもデバッグもとても効率よく行うことができます。
ちなみにこの2WaySQL、発祥はあの有名なSeasar2です。

2. パラメタの値に応じたSQL文の変化
clione-sqlでは、パラメタの値に応じて自動的にSQL文を変化させることができます。
例として、下記のようなケースを考えます。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 IN /* param */('aaa', 'bbb', 'ccc')

paramの値がsizeが5の配列(or List)だった場合、SQL文は下記のように変換されます。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 IN (?, ?, ?, ?, ?)

このとき、配列(or List)の保持する値は順番どおり正しくbindされます。
また、INで判定したい値に固定のものがあった場合には、下記のように書くことも可能です。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 IN ('aaa', 'bbb', /* param */'ccc')

この場合も、paramの値がsizeが5の配列(or List)だったら、下記のように変換され、保持する値も正しくbindされます。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 IN ('aaa', 'bbb', ?, ?, ?, ?, ?)


更に、SQLファイルで下記のように「=」の前にパラメタを記述すると、パラメタの値に合わせて条件文ごとSQL文を変化させることができます。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 /* param */= 100

paramがnullのときは、下記のように「IS NULL」に変化します。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 IS NULL


paramが配列(or List)でsizeが2以上の場合には、下記のようにIN句に変化します。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 IN (?, ?, ?, ?, ?)

※配列(or List)の要素数が1のときには「=」に、0の時には「IS NULL」に変化します。

更にOracleの場合には、IN句に1000を超えるパラメタを指定できないため、1000を超えたら自動的に下記のように1000件ずつに分けて、ORを併用して展開します。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?....)

    OR FIELD1 IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?....)

    OR FIELD1 IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

上記のように動作するのは、パラメタを「=, IS, IN, <>, !=, IS NOT, NOT IN」等の比較演算子の前においた場合です。
なお「<>」など否定の比較演算子の前にパラメタを置いた場合には、nullの時には「IS NOT NULL」に、Listか配列の時には「NOT IN」に、それぞれ変化します。

「LIKE」の前にパラメタを置いた場合もほぼ同じ動作をしますが、パラメタがList or 配列のときの挙動が違います。
デフォルトでは、下記のように「OR」に展開します。

SELECT

    *

FROM

    TABLE1

WHERE

    FIELD1 LIKE ?

    OR FIELD1 LIKE ?

    OR FIELD1 LIKE ?

    OR FIELD1 LIKE ?

    OR FIELD1 LIKE ?

パラメタを調整することで「AND」に展開させることもできますが、これについては後述します。

3. SQLファイルの配置
SQLファイルは基本的にクラスパスの通ったディレクトリ以下のどこかに配置します。
配置した場所のパスを、下記のように指定します。

import static tetz42.clione.SQLManager.*;

           :

        sqlManager().useFile("com/sql/Sample.sql")

                .find ...(省略)

    }

またSQLファイルをクラスと関連付けて配置したいときのために、SQLManager#useFileメソッドではClassオブジェクトをパラメタに渡せるようになっています。

import static tetz42.clione.SQLManager.*;

           :

        sqlManager().useFile(SampleDao.class, "Sample.sql")

                .find ...(省略)

    }

上記の例では、例えばSampleDaoのパッケージが「com.clione.dao」だったとすると、

com/clione/dao/sql/SampleDao/Sample.sql
に配置されているとみなされます。つまり、
[渡されたClassオブジェクトのパッケージ]/sql/[クラス名]/[指定されたファイル名]
というパスだと解釈されますので、そのパスに適切にSQLファイルを置くようにしてください。
4. ファイルを使用しないSQL
SQLファイルを作成するほどでもない簡単なSQL文の場合には、下記のように直接実行することもできます。

import static tetz42.clione.SQLManager.*;

           :

    public void delete() {

        return sqlManager().useSQL("DELETE FROM TABLE1").update();

    }

もちろん、パラメタも使えます。

import static tetz42.clione.SQLManager.*;

           :

    public Entity findById(String id) {

        return sqlManager().useSQL("SELECT * from TABLE1 WHERE ID=/* id */")

                  .find( Entity.class, params("id", id) );

    }

なおパラメタに渡すSQL文には、ユーザが入力した値やDBから取得した値などの外部の値を使用しないでください。
上記を守らないと、SQLインジェクションという攻撃に対する脆弱性となってしまう可能性があります。

5. インデントベースの動的SQL
clione-sqlの名前の由来は、「Clione-sql is Line and Indent Oriented, NEsted-structured, 2WaySQL library.」です。
つまり「clione-sqlは行志向かつインデント志向で、再帰的な構造を持つ2WaySQLなライブラリです。」ってな感じです。
ここではこの名前の由来となった、行志向かつインデント志向な動的SQLについて説明します。

まずは下記のSQLファイルをご覧ください。

SELECT

    *

FROM

    people

WHERE

    age >= /* $age_from */25

    AND age <= /* $age_to */50

最初の例と比べると、パラメタに「$」という記号がついていることが分かると思います。
これをclione-sqlでは パラメタ修飾記号 と呼びます。
下記のように、Javaソースでパラメタを指定するときには、パラメタ修飾記号は省略できます。

import static tetz42.clione.SQLManager.*;

        :

    public List<Person> findAllByAge(Integer ageFrom, Integer ageTo) throws SQLException {

        return sqlManager().useFile(getClass(), "Select.sql")

                .findAll(Person.class, params("age_from", ageFrom).$("age_to", ageTo));

    }

なお、パラメタ修飾記号を省略せずに書いても正常に動作しますが、内部的にはパラメタ修飾記号は除去されます。

パラメタ修飾記号には幾つか種類があり、それぞれで意味が違います。
「$」は「パラメタの値がnullだったら、行ごと削除する」という意味になります。
※ 厳密には少々違いますが、ここでは説明を簡単にするため上記のように書いています。詳細は次の「negativeとpositive」を参照ください。
よって、もしパラメタage_fromがnullだった場合、SQL文は下記のように変換されます。

SELECT

    *

FROM

    people

WHERE

    age <= ?

区切りの「AND」も除去されて正しいSQL文になっていることに気がついたでしょうか？
これはclione-sqlが自動的に行っています。

age_from, age_to共にnullだった場合は下記のようにWHERE句ごと除去されます。。

SELECT

    *

FROM

    people

5-1. Clione-SQLのルール
clione-sqlでは以下のルールに従って、これを実現しています。

インデントに従って、下記のように親ノード・子ノードを定義する
親ノード１

    子ノード１−１

    子ノード１−２

        孫ノード１−２−１

        孫ノード１−２−２

    子ノード１−３

親ノード２

    子ノード２−１

    子ノード２−２

子ノードが全て除去された場合、親ノードも除去される。上記例では、孫ノード１−２−１、孫ノード１−２−２が除去された場合には子ノード１−２が除去され、更に子ノード１−１、子ノード１−３も除去された場合には親ノード１が除去される。
隣接するノードで同一インデントのものを纏めて、「ブロック」と呼ぶ。ただし親ノード１、２や、子ノード１−２、１−３のように、間にあるのが子ノードのみである場合は、隣接しているものとみなすので同じブロックに属するノードである。
最初の状態でブロックの先頭がSQL文の区切り(「AND」, 「OR」,「,」など)でない場合、ノード除去後のブロックの先頭が区切りになっていたら、その区切りを除去する。
もう一度最初の例に戻って説明すると、

SELECT

    *

FROM

    people

WHERE  -- 親ノード１

    age >= /* $age_from */25      -- 子ノード１−１

    AND age <= /* $age_to */50  -- 子ノード１−２

子ノード１−１のみが除去された場合ブロックの先頭が区切りではないため、先頭になった子ノード１−２の先頭の「AND」が除去されます。
子ノード１−１、１−２がともに除去されたとき、その親ノードである親ノード１が除去されます。

また、

親ノードが除去された場合、その親ノードに属する子ノード全てが除去される
というルールもあるため、下記のようにブロック単位でSQL文を切り替えるようなことも可能です。
SELECT

    *

FROM

    employee

WHERE

    title = /* $title */'chief'

    -- %IF useDateEmployed

      AND /* $date_from */'19980401' <= date_employed

      AND date_employed  <= /* $date_to */'20050401'

    -- %ELSE

      AND /* $date_from */'20080401' <= date_of_promotion

      AND date_of_promotion  <= /* $date_to */'20120401'

ORDER BY

    employee_id

※ 「%IF」、「%ELSE」については後述。

その他にもVersion 0.5.0から、ルールが二つ追加になりました。そのうちの一つが、
Clione-SQLが処理を行う前のブロックの最後がSQL文の区切り(「AND」, 「OR」,「,」など)でない場合に、処理後のブロックの最後が区切りになっていたら、その区切りを除去する。
というものです。このルールにより、
SELECT

    *

FROM

    people

WHERE 

    age >= /* $age_from */25 AND

    age <= /* $age_to */50

のように、区切りを行の最後に持ってくるフォーマットも正しく処理できるようになりました。
上記例で「age_to」がnullだった場合は行ごと削除されて「age >= /* $age_from */25 AND」がブロックの最後の行になりますが、上記ルールにより行末の「AND」が除去されて、
SELECT

    *

FROM

    people

WHERE 

    age >= ?

というSQL文に変換されます。

もう一つのルールは、
SQL文の区切り(「AND」, 「OR」,「,」など)のみの行は、次の行と結合して一つのノードとして扱う
というものです。このルールにより、
SELECT

    *

FROM

    people

WHERE 

    age >= /* $age_from */25

    AND

    age <= /* $age_to */50

というフォーマットのSQL文も正しく処理できます。
上記は「UNION」、「UNION ALL」を意識したルールで、例えば下記のようなSQL文を例として考えます。
SELECT /* &table1 */

        id

        ,name

        ,type

    FROM

        table1

UNION

SELECT /* &table2 */

        id

        ,name

        ,type

    FROM

        table2

UNION ALL

SELECT /* &table3 */

        id

        ,name

        ,type

    FROM

        table3

「UNION」、「UNION ALL」はClione-SQLでは「AND」等と同様に区切りとして扱われます。
よって上記例の「UNION」、「UNION ALL」のみの行は、その下のSELECT句と同一ノードとして扱われます。
SELECTの後ろに「&」というパラメタ修飾記号がついたパラメタがそれぞれついています。
「&」は「$」と同様、「パラメタの値がnullだったら、行ごと削除する」という動作は行いますが、「$」とは違ってプレースホルダの「?」の付与とパラメタのバインドを行いません(※「&」の詳細は後述)。

上記例でtable1がnullの場合、親ノードが除去されればその子ノードは全て除去されるので、table1のSQL文は全て、除去されて、table2, 3のSQL文が残されます。
このときtable2のSQL文の先頭は「UNION」という区切りなので除去されて、結果下記のようになります。
SELECT 

        id

        ,name

        ,type

    FROM

        table2

UNION ALL

SELECT 

        id

        ,name

        ,type

    FROM

        table3


またtable2がnullだった場合には、table2のSQL文がその直前のUNIONごと除去され下記のようになります。
SELECT 

        id

        ,name

        ,type

    FROM

        table1

UNION ALL

SELECT 

        id

        ,name

        ,type

    FROM

        table3

5-2. インデントについて
Clione-SQLでは、デフォルトでは4タブ設定のエディタの見た目通りにインデントを計算します。
よってインデントに半角空白とタブが混在していても、エディタでの見た目を確認しながら作業をすれば簡単に正しく動作するSQLファイルが作成可能です。
また例えば8タブなど違うタブ数のエディタで作業をしたければ、設定ファイルで任意の値に設定することができます。(※ 後述)

SQL文を書くときに、適切な単位で改行したり意味のある単位でインデントするのは、開発効率を上げるために多くの方がやっていることだと思います。
clione-sqlではこれに着目して、
「正しく改行・インデントしてあるSQL文であれば、適切なパラメタ修飾子を付けるだけで安全かつ簡単に動的SQLを実現できる」
という世界観を目指しています。
6. SQL文のパースのルール
Clione-SQLではSQL文の文字列・括弧を認識して、可能な限りユーザの意図に則った処理の実現を目指しています。
ここでは、Clione-SQLがどのようにSQL文を解釈しているのか学びます。

6-1. 文字列リテラルのパース
ANSIによると、SQL文中に登場する文字列リテラルとしては、下記のようなものが認められています。

-- 通常の文字列リテラル

'a normal string'



-- 改行コードを含む文字列リテラル

'a string

which

contains

CRLF'



-- エスケープを含む文字列リテラル

'It''s a string which contains escape sequence'

Clione-SQLでは、上記を正しく認識して処理を行います。
よって文字列リテラルの途中で改行していても、エスケープが含まれていても問題なくパースされます。

例として、下記のSQLでパラメータの「ADDRESS」がnullだった場合を考えます。

INSERT INTO PEOPLE (

        ID

        ,NAME

        ,ADDRESS /* &ADDRESS */

) VALUES (

        /* ID */'0001'

        ,/* NAME */'Yoko'

        ,/* $ADDRESS */'Ocian-Child''s House

123-4

Dokoka-cho

Asoko-ku

Tokyo pref.

Japan'

)

文字列リテラルは正しく解釈されますので、上記最後の文字列リテラルには改行コードとエスケープが含まれていますが、Clione-SQLでは改行コードなどを無視し正しく一つの文字列リテラルであると解釈するため、「/* $ADDRESS */」のある行以下6行を一つの行として解釈します。

よってこれは、
INSERT INTO PEOPLE (

        ID

        ,NAME

) VALUES (

        ?

        ,?

)

というSQLに変換されます。

もし下記のようにエスケープし忘れなどで「'」の整合性が取れていない、不正なSQL文を検出した場合にはClioneFormatExceptionをthrowします。

SELECT

  *

FROM

  BOOKS

WHERE

  TITLE like /* keyword */'%'s%'

なおMySQLやPostgreSQLでは、下記のような「\」を使ったエスケープがありますが、現時点のClione-SQL(バージョン0.5.1)では無視されます。
よって、下記は文字列の整合性が取れていないと判断され、上記同様ClioneFormatExceptionがthrowされます。

SELECT

  *

FROM

  BOOKS

WHERE

  TITLE like /* keyword */'%\'s%'

6-2. 括弧のパース
Cione-SQLでは、括弧も意識してパースが行われています。

SELECT

    *

FROM

    EMPLOYEE

WHERE

    TITLE = /* $title */'Chief'

    AND (

        ENTERING_DATE <= /* $today */'2012-01-01'

        OR RETIRE_DATE  >= /* $today */'2012-01-01'

    )

5-5. パラメタ修飾記号の否定
パラメタ修飾記号は、「!」を付与することで挙動を逆にすることもできます。
SELECT

    *

FROM

    people

WHERE

    hair_color = 'black'

    AND age /* $!age */= 25

上記のように書いた場合、ageがnullの時には「age IS NULL」に変換され、ageに値が入っているときには行削除が行われます。
6. negativeとpositive
実は上の「インデントベースの動的SQL」の中で書いた、「nullのときには･･･」という表現、厳密には間違っています。
clione-sqlではパラメタの値に関してnegativeかpositiveかを判定します。このnegativeと判定される値の一つが「null」です。
clione-sqlにてnegativeと判定される値は下記の通りです。
null
false(より正確には、Boolean.FALSEとequals判定で一致するもの)
空の配列 or List
要素が全てがnegativeの配列 or List
その他、ユーザーがnegativeとして設定した値(※応用編にて後述)
上記以外の値は、全てpositiveとして判定されます。
※以降の説明では、この「negative」「positive」という用語を使います。


7. パラメタ修飾記号
ここで、パラメタ修飾記号について詳しく説明します。
パラメタ修飾記号はパラメタの前につけることで特別な意味を持たせることができる記号のことです。
上でも触れていますが、パラメタとの間に「!」をつけることで意味を逆転させることができます。


$ ･･･ パラメタがnegativeだったときに行ごと削除します。(前述)
由来：正規表現では行末を表す記号なので、行の制御をつかどらせることにしました。
& ･･･ パラメタがnegativeだったときに行ごと削除するのは「$」と同じです。違いはパラメタがpositiveのときに
「?」のSQL文への付与
値のbind
などの処理を行わないことです。下記みたいなケースで使います。
SELECT

    *

FROM

    people

WHERE

    SEX = 'female' /* &!is_gender_free */

    AND age /* $!age */= 25

由来：上のように、条件分っぽい使い方になると思ったので、「and」を意味するこれを選びました。
@ ･･･ パラメタがnegativeのときに、ParameterNotFoundExceptionをthrowします。必須のパラメタに付与します。
由来：特にないです。なんとなく必須っぽいイメージがわきました。
? ･･･ パラメタがnegativeのときに、右隣のパラメタを有効とします。右隣のパラメタもnegativeで、更にその右隣のパラメタがなかったら、パラメタの後ろの値を有効とします。つまり、
UPDATE people

SET

    hometown = /* ?prefecture ?country */'unknown'

WHERE

    ID = /* @id */'11'

となっていた場合、prefecture の値がpositiveならその値が、negativeならcountry が、countryもnegativeなら後ろの値の'unknown'が有効となります。
もし、
hometown = /* ?prefecture ?country */
と、後ろの値がないときに両方ともnegativeなら、何も起こりません。
由来：これも特にないです。イメージです。
6. SQLコメントの扱い
6-1. SQLコメント内の改行
SQLコメント内部では、自由に改行することができます。

{{{}}}


5-4. 行コメントの扱い
5-4. 改行のエスケープ
様々なfindメソッド
// TODO 説明追記


INSERT, UPDATE, DELETEを実行するupdateメソッド
// TODO 説明追記


パラメタについて
paramsメソッド
条件bean使用
$, $e, $on
コネクション
// TODO 説明追記


補助関数(%concat, %C, %esc_like, %L)
clione-sql0.4.0より、補助関数という概念が導入されました。
パラメタ修飾記号だけでは対処が難しいことを実現します。
補助関数には多数種類がありますが、基礎編では4つだけ紹介します。


文字列連結補助関数(%concat, %C)
「%concat」は文字列連結用の補助関数です。与えられた文字列とパラメタを連結して、一つのパラメタとしてまとめる働きがあります。
サンプルとして、下記のSQLファイルをご覧ください。

SELECT

    COUNT(*)

FROM

    people

WHERE

    name like /* %concat('%', part_of_name, '%') */'%愛%'

これは下記のように変換されます。

SELECT

    COUNT(*)

FROM

    people

WHERE

    name like ?

part_of_nameの値が「希」だったとすると、bindされるパラメタの値は「%希%」になります。
なお、括弧と括弧内の区切り文字「,」は省略することもできます。またより短くしたい場合には、「%C」が「%concat」と同じ働きをする補助関数なので、こちらを使用してください。下記は上と全く同じ意味のSQLファイルです。

SELECT

    COUNT(*)

FROM

    people

WHERE

    name like /*%C '%' part_of_name '%' */'%愛%'

%concat, %Cのその他の使い方として、各種DBプロダクトに依存しないで文字列連結を行う、というのがあります。
文字列連結はANSI標準では「string1 || string2」なのですが、MySQLはconcat関数にしか対応していなかったり、SQL Serverは「string1 + string2」だったり、実情は全く統一されていません。
%concat, %Cを使えば下記のように、DBプロダクトに依存しない形で文字列連結を実現することができます。

INSERT INTO people (

    id

    ,name

    ,last_name

    ,full_name

    ,full_name_jp

)

values(

    11

    ,/* name */

    ,/* last_name */

    ,/*%C name ' ' last_name */

    ,/*%C last_name '　' name */

)

LIKE句用補助関数(%esc_like, %L)
LIKE句に値をbindするとき、値の中に'%'や''が含まれていると、LIKE句の記号として解釈されてしまうことが知られています。
※ 念のため：''はLIKE句では任意の一文字にマッチします。

よって例えば「そうさ100%病気」をLIKE句で検索した場合、

そうさ100年間病気
そうさ100人全部病気
など、想定とは違う検索結果も一緒に返ってきてしまいます。
これは場合によっては、全く関係ない人に重要情報を見せてしまうようなセキュリティバグにつながりかねません。
これを避けるため、ANSIでLIKE句のエスケープを下記のように定めています。
下記は「そうさ100%病気」という文字列が含まれるレコードの件数を数える例です。
LIKE句で意味を持つ'%'の前に'#'を置いてエスケープしています。
SELECT

    COUNT(*)

FROM

    songs

WHERE

    lyrics like '%そうさ100#%病気%' escape '#'

※ 本来escapeに使う値はユーザが自由に決めて良いのですが、clione-sqlでは「#」に統一しています。

このエスケープを行う補助関数が%esc_likeです。
下記のようにして使用します。
SELECT

    COUNT(*)

FROM

    songs

WHERE

    lyrics like /*%C '%' %esc_like(part_of_lyrics) '%' */'%そうさ100#%病気%' escape '#'

part_of_lyricsに渡した値が、自動的に'#'でエスケープされるようになります。

補助関数「%L」を使うと上記は下記のように、より簡単に書くことができます。
SELECT

    COUNT(*)

FROM

    songs

WHERE

    lyrics like /*%L '%' part_of_lyrics '%' */'%そうさ100#%病気%'

「%L」は、与えられたパラメタをエスケープし、%concatで連結した上で、SQL文に自動で「escape '#'」を付与します。
また、パラメタの数が下記のように複数になっても、それぞれのパラメタに%esc_likeを適用するので大丈夫です。
SELECT

    COUNT(*)

FROM

    songs

WHERE

    lyrics like /*%L '%' pol1 '_' pol2 '%' */'%そうさ100#%病気%'

pol1が「君」、pol2が「1000%」だった場合、上記のパラメタは連結されて「%君_1000#%%」に、SQL文は下記のように変換されます。
SELECT

    COUNT(*)

FROM

    songs

WHERE

    lyrics like ? escape '#'

検索結果として、
君が1000%
君も1000%
君と1000%
などが含まれたレコードの件数が取得できるはずです。

LIKE句に値をbindするときには、エスケープの必要がなければ%Cを、あれば%Lを使ってください。
いちいち判断するのが面倒なら、「LIKE句だったら必ず『%L』」としても良いでしょう。
応用編
文字列リテラル
Coming soon!

  'string'

  "field1 = /+ field1 +/"

  :field1 = /+ field1 +/

  |field1 = /* field1 */

パラメタの合成
// TODO 説明の追記


コメント
Coming soon!

-- コメント

  /** 通常のコメントと解釈 */

  -- こちらも通常のコメント

  /*+ Oracleのヒント句 */

  /*! MySQLのヒント句 */

-- パラメタ

  /* param */

  -- &param

行の連結
Coming soon!

  FIELD IN /* values */('aaa', 'bbb', 'ccc' --

      'ddd', 'eee')

  aaa in /**

    この書き方のコメントは、

    複数行に分けて書いても、1行だと判断されます。

    改行文字もコメントアウトされる、と考えると分かり易いと思います。

    */ ('111', '222', '333')

SQLExecutor
negativeと判定する値の追加
propertiesファイル
%if-%elseif-%else
   /*%if cond1 'AAA' %elseif cond2 'bbb' else 'ccc' */'ddd'

ブロック切替え
WHERE

  -- %if cond

    fieldA1 = /* valueA1 */

    AND fieldA2 = /* valueA2 */

  -- %if !cond

    --: fieldB1 = /+ valueB1 +/

    --: AND fieldB2 = /+ valueB2 +/

  /* %if condC

    fieldC1 = /+ valueC1 +/

    AND fieldC2 = /+ valueC2 +/

  */

%include
    /* %include('./Sub_Query') '

        UNION

'      %include('./Sub_Query' %on('option1')) */

%STR, %SQL
// TODO より詳細な説明
JavaからSQLに直接文字列 or clione-sqlにより解釈されたSQL文を書き込むことができる補助関数。
使い方を間違えるとSQLインジェクションをくらう危険性があるので、要注意！
別の手段がどうしてもないときのみ使用し、使用する場合もなるべくJavaの固定値(static finalな値とかenumとか)を渡すようにしてください。
間違っても、ユーザの入力した値をノーチェックで渡さないこと！
※警戒の意味を込めて、この二つの補助関数だけは1文字でもないのに大文字で表記しています。

    /* %STR(param1) */

    /* %SQL(param2) */
