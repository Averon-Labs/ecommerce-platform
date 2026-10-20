# برنامه اجرایی لود تصویر — بخش داشبورد ادمین و بک‌اند
# (Image Loading Execution Plan — Admin & Backend)

این سند اختصاصاً شامل چک‌لیست و گام‌های اجرایی لود بهینه تصاویر و تقدم رندر متن در **پنل ادمین (React)** و **قراردادهای وب‌سرویس بک‌اند (REST API)** است.

> **سندهای مرتبط:**
> - اصل معماری: [`docs/ARCHITECTURE.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/ARCHITECTURE.md#L112-L142)
> - برنامه اجرایی بخش اندروید: [`docs/IMAGE_LOADING_ANDROID.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/IMAGE_LOADING_ANDROID.md)
> - اسکیل اختصاصی: [`.agents/skills/text-first-async-image-loading/SKILL.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/.agents/skills/text-first-async-image-loading/SKILL.md)

---

## فاز ۱: پنل مدیریت تحت وب ادمین (Admin Web Dashboard)

### گام ۱ (گام ۹ کلی): استانداردهای لود تصاویر و رندر متنی در پنل ادمین
* **مسیر هدف:** `admin/` (پروژه وب با React / TypeScript / Tailwind)
* **هدف:** تضمین رندر بلادرنگ داده‌های متنی جدول محصولات بدون معطلی برای لود عکس‌ها و جلوگیری کامل از پرش چیدمان (CLS = 0) در وب.
* **چک‌لیست اجرایی:**
  - [ ] در جداول مدیریت محصولات (`Product Management Table`)، ستون‌های داده‌های متنی (کد SKU، عنوان، دسته‌بندی، قیمت، موجودی انبار) بلادرنگ و بدون وقفه رسم شوند.
  - [ ] برای تامبنیل‌های کوچک در جدول از اتریبیوت‌های نیتیو مرورگر `loading="lazy"` و `decoding="async"` استفاده شود:
    ```tsx
    <div className="relative aspect-square w-12 overflow-hidden rounded-md bg-sandstone-100">
      <img
        src={product.imageUrl}
        alt={product.name}
        loading="lazy"
        decoding="async"
        className="h-full w-full object-cover transition-opacity duration-200"
      />
    </div>
    ```
  - [ ] برای تمامی عکس‌ها نسبت ابعاد ثابت (مانند `aspect-square` یا ابعاد صریح پیکسل `w-12 h-12`) ست شود تا از Cumulative Layout Shift (CLS) در وب جلوگیری گردد.
  - [ ] در فرم‌های ساخت و ویرایش کالا، پیش‌نمایش تصویر دارای افکت Skeleton Pulse تا زمان لود کامل باشد.

---

## فاز ۲: قراردادها و اعتبارسنجی بک‌اند (Backend REST API)

### گام ۲ (گام ۱۰ کلی): اعتبارسنجی و پالایش پاسخ‌های JSON در بک‌اند
* **فایل‌های هدف:**
  - [`docs/API_CONTRACT.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/API_CONTRACT.md)
  - [`docs/openapi.yaml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/openapi.yaml)
  - روت‌ها و کنترلرهای بک‌اند در `backend/`
* **هدف:** جداسازی کامل فراداده‌های متنی از باینری تصویر در پاسخ‌های API برای تضمین پاسخگویی زیر ۱۰۰ میلی‌ثانیه.
* **چک‌لیست اجرایی:**
  - [ ] تمام اندپوینتهای لیستی (`GET /api/v1/products`، `GET /api/v1/cart`، `GET /api/v1/orders`) فقط آدرس رشته‌ای متنی تصویر (`imageUrl` / `thumbnailUrl`) را برگردانند.
  - [ ] ارسال باینری عکس، استریم‌های خام یا رشته‌های سنگین Base64 در داخل پاسخ‌های JSON اکیداً مسدود و فیلتر شود.
  - [ ] در صورت امکان، نسبت ابعاد تصویر یا ابعاد مرجع (`width` و `height`) در مدل به عنوان فراداده ارسال شود تا کلاینت‌ها بدون دانلود عکس ابعاد را بدانند.
  - [ ] هدرهای کش HTTP برای سرور تصاویر استاتیک روی زمان بالا تنظیم باشند (`Cache-Control: public, max-age=31536000, immutable`).

---

## جدول پیگیری وضعیت ادمین و بک‌اند

- [ ] **گام ۹ (ادمین):** پیاده‌سازی لود تنبل و کانتینر ثابت در پنل ادمین (React)
- [ ] **گام ۱۰ (بک‌اند):** اعتبارسنجی قرارداد JSON و هدرهای کش در بک‌اند (REST API)
