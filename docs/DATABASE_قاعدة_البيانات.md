# قاعدة البيانات — Room (SQLite)

- المحرّك: **Room 2.6.1** فوق SQLite.
- اسم الملف: `whatsapp_reminder.db`
- الإصدار الحالي: **4**
- `exportSchema = false`
- التعريف: `data/local/database/AppDatabase.kt` · الكيان: `ReminderEntity.kt` · الوصول: `ReminderDao.kt`

---

## الجدول: `reminders`

| العمود | النوع | ملاحظات |
|---|---|---|
| `id` | INTEGER | مفتاح رئيسي، `autoGenerate` |
| `contact_name` | TEXT | اسم جهة الاتصال (غير فارغ) |
| `phone_number` | TEXT | الرقم مع رمز الدولة (مثال `+9665xxxxxxxx`) |
| `message` | TEXT | نصّ الرسالة |
| `scheduled_time` | INTEGER | وقت التذكير (epoch millis) |
| `status` | TEXT | اسم `ReminderStatus` |
| `created_at` | INTEGER | وقت الإنشاء (epoch millis) |
| `notified_at` | INTEGER? | وقت الإشعار (null إن لم يُرسل) |
| `notes` | TEXT | ملاحظات (افتراضي `''`) |
| `sound_enabled` | INTEGER | 1/0 — صوت لكل تذكير (افتراضي 1) — أُضيف في v2 |
| `open_whatsapp_directly` | INTEGER | 1/0 — فتح واتساب مباشرة (افتراضي 1) — أُضيف في v2 |
| `recurrence` | TEXT | اسم `RecurrenceType` (افتراضي `NONE`) — أُضيف في v3 |
| `category` | TEXT | تصنيف (افتراضي `''`) — أُضيف في v3 |
| `recurrence_days` | INTEGER | قناع أيام الأسبوع للتكرار المخصص (بت 0=الأحد … بت 6=السبت) (افتراضي 0) — أُضيف في v4 |
| `recurrence_end` | INTEGER? | نهاية التكرار (null = بلا نهاية) — أُضيف في v4 |

### قيم `status` (ReminderStatus)
`SCHEDULED` · `PENDING_NETWORK` · `NOTIFIED` · `OPENED` · `CANCELLED` · `EXPIRED`

### قيم `recurrence` (RecurrenceType)
`NONE` · `DAILY` · `WEEKLY` · `MONTHLY` · `CUSTOM_DAYS`

---

## SQL المكافئ لإنشاء الجدول (نسخة v4)
```sql
CREATE TABLE reminders (
  id                     INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  contact_name           TEXT    NOT NULL,
  phone_number           TEXT    NOT NULL,
  message                TEXT    NOT NULL,
  scheduled_time         INTEGER NOT NULL,
  status                 TEXT    NOT NULL,
  created_at             INTEGER NOT NULL,
  notified_at            INTEGER,
  notes                  TEXT    NOT NULL DEFAULT '',
  sound_enabled          INTEGER NOT NULL DEFAULT 1,
  open_whatsapp_directly INTEGER NOT NULL DEFAULT 1,
  recurrence             TEXT    NOT NULL DEFAULT 'NONE',
  category               TEXT    NOT NULL DEFAULT '',
  recurrence_days        INTEGER NOT NULL DEFAULT 0,
  recurrence_end         INTEGER
);
```

---

## الترحيلات (Migrations)
مسجّلة في `AppDatabase` وتُربط عند بناء القاعدة في `di/AppModule.kt`.

**1 → 2:** إضافة إعدادات لكل تذكير
```sql
ALTER TABLE reminders ADD COLUMN sound_enabled INTEGER NOT NULL DEFAULT 1;
ALTER TABLE reminders ADD COLUMN open_whatsapp_directly INTEGER NOT NULL DEFAULT 1;
```
**2 → 3:** التكرار والتصنيف
```sql
ALTER TABLE reminders ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE';
ALTER TABLE reminders ADD COLUMN category  TEXT NOT NULL DEFAULT '';
```
**3 → 4:** أيام التكرار المخصّصة ونهايته
```sql
ALTER TABLE reminders ADD COLUMN recurrence_days INTEGER NOT NULL DEFAULT 0;
ALTER TABLE reminders ADD COLUMN recurrence_end  INTEGER;
```

> عند إضافة عمود جديد لاحقاً: ارفع `version` في `@Database`، أضِف `Migration(n, n+1)`،
> وسجّله في `AppModule`. لا تعتمد على `fallbackToDestructiveMigration` في الإنتاج.

---

## استعلامات DAO الرئيسية (`ReminderDao`)
- `getAllReminders(): Flow<List<ReminderEntity>>` — مرتّبة تصاعدياً حسب `scheduled_time`.
- `getReminderById(id): Flow<ReminderEntity?>` — تدفّق لتذكير واحد.
- `getReminderByIdOnce(id)` / `getRemindersByStatus(status)` — لمرة واحدة (المستقبِلات/الإقلاع).
- `insert` (REPLACE) · `update` · `updateStatus(id, status, notifiedAt)` · `delete` · `deleteAll` (المسح الشامل).

---

## النسخ الاحتياطي (JSON)
`data/backup/ReminderBackup.kt` يصدّر/يستورد التذكيرات كـ JSON (تصدير/استيراد من الإعدادات).
لا يشمل سجلّ التفعيل (يبقى في تخزين خاصّ منفصل `app_guard`).
