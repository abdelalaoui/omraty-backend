-- Les 6 conditions actuelles de l'app (texte arabe exact, voir
-- lib/features/omra/data/room_booking_terms.dart), dans le même ordre, pour que le déploiement de
-- cette carte ne change rien de visible pour les utilisateurs (voir critère d'acceptation).
INSERT INTO booking_term (text_ar, display_order, enabled) VALUES
    ('١-أن يكون المعتمر لم يسبق له الترحيل أي باللغة الوطنية( مسفر ) ، وألا يكون ممنوع من دخول المملكة.', 1, TRUE),
    ('٢-الفترة الزمنية التي تم إختيارها من طرف المعتمر، غير قابلة لتغيير أو التمديد.', 2, TRUE),
    ('٣-عدد الأسرة في الغرفة التي تم إختيارها غير قابلة للتغيير.', 3, TRUE),
    ('٤-لا يمكن للمعتمر تغيير مسار الرحلة.', 4, TRUE),
    ('٥-لا يمكن للمعتمر إقتناء وسيلة نقل أخري بدل النقل المبرمج من طرف الوكالة.', 5, TRUE),
    ('٦-قبل 72 ساعة من المعاملة، يمكن للمعتمر إلغائها مع أخذ الرسوم البنكية بعين الاعتبار، أما بعدها فلا يمكن للمعتمر إلغاء المعاملة مع إمكانية تغيير شخصه باخر من نفس الجنس.', 6, TRUE);
