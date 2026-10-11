# 지원금 데이터 모델(JSON 직렬화)이 난독화로 깨지지 않도록 유지
-keep class kr.example.subsidycompare.data.** { *; }
-keepattributes *Annotation*, InnerClasses
