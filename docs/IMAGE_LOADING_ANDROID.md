# برنامه اجرایی لود تصویر — بخش اندروید
# (Image Loading Execution Plan — Android Customer Application)

این سند اختصاصاً شامل چک‌لیست و گام‌های اجرایی لود بهینه تصاویر و تقدم رندر متن در **اپلیکیشن اندروید (Java / XML / Material 3)** است.

> **سندهای مرتبط:**
> - اصل معماری: [`docs/ARCHITECTURE.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/ARCHITECTURE.md#L112-L142)
> - قوانین اختصاصی اندروید: [`android/AGENTS.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/AGENTS.md#L121-L135)
> - برنامه اجرایی بخش ادمین و بک‌اند: [`docs/IMAGE_LOADING_ADMIN_BACKEND.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/IMAGE_LOADING_ADMIN_BACKEND.md)
> - اسکیل اختصاصی: [`.agents/skills/text-first-async-image-loading/SKILL.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/.agents/skills/text-first-async-image-loading/SKILL.md)

---

## فاز ۱: زیرساخت و ابزار مرکزی اندروید (Android Core Tooling)

### گام ۱: ارتقا و پاکسازی خودکار منابع در `ImageLoaderHelper`
* **فایل هدف:** [`android/app/src/main/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelper.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelper.java)
* **چک‌لیست اجرایی:**
  - [x] متد `clear(@Nullable ImageView imageView)` فراخوانی شده و تمامی ریکوئست‌های در حال اجرای Glide برای ویوهای بازیافت‌شده را متوقف کند.
  - [x] استراتژی کش روی دیسک به صورت صریح روی `DiskCacheStrategy.ALL` تنظیم باشد تا تمام سایزهای دیکودشده و باینری اصلی ذخیره شوند.
  - [x] کش حافظه رم فعال باشد (`skipMemoryCache(false)`) تا در اسکرول‌های سریع بالا و پایین، زمان بازخوانی تصویر ۰ میلی‌ثانیه باشد.
  - [x] ترنزیشن انیمیشن تصویر روی `DrawableTransitionOptions.withCrossFade(250)` ثابت و نرم تنظیم باشد.
  - [x] اندازه دیکود به صورت سخت‌افزاری بر اساس ابعاد فیزیکی `ImageView` تنظیم شود تا از خطای OutOfMemory جلوگیری شود.

### گام ۲: تست‌های واحد و یکپارچه زیرساخت لودر
* **فایل هدف:** [`android/app/src/test/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelperTest.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/test/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelperTest.java)
* **چک‌لیست اجرایی:**
  - [x] تست نال‌سیفتی برای URLهای خالی یا `null` پاس شود و تصویر خطای پیش‌فرض لود گردد.
  - [x] تست پیش‌بارگذاری گروهی (`preloadBatch`) با لیست‌های ترکیبی از آیتم‌های معتبر، خالی و نال اعتبارسنجی شود.
  - [x] تست ثابت زمانی انیمیشن CrossFade روی دقیقاً `250ms` تثبیت و پاس شود.
  - [x] اجرای فرمان `./gradlew testDebugUnitTest --tests ImageLoaderHelperTest` بدون هیچ‌گونه خطا پایان یابد.

---

## فاز ۲: آداپتورها و ویوپورت‌های کاتالوگ و خرید (Android Adapters & Layouts)

### گام ۳: کاتالوگ و گرید محصولات
* **فایل‌های هدف:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/ProductGridAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/ProductGridAdapter.java)
  - [`android/app/src/main/res/layout/item_product_card.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_product_card.xml)
  - [`android/app/src/main/res/layout/item_product_linear_card.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_product_linear_card.xml)
* **چک‌لیست اجرایی:**
  - [x] در متد `bind()`، نام محصول، قیمت فرمت‌شده با پول رسمی، بج موجودی و تگ دسترس‌پذیری در خطوط اول و به شکل همگام (Synchronous) ست شوند.
  - [x] متد `onViewRecycled` در `ProductGridAdapter` اورراید شود و متد `ImageLoaderHelper.clear(holder.binding.ivProductImage)` برای لغو دانلود عکس‌های خارج از کادر فراخوانی گردد:
    ```java
    @Override
    public void onViewRecycled(@NonNull ProductViewHolder holder) {
        super.onViewRecycled(holder);
        ImageLoaderHelper.clear(holder.binding.ivProductImage);
    }
    ```
  - [x] در فایل‌های XML، ویژگی `app:layout_constraintDimensionRatio="1:1"` روی ویوپورت تصویر قرار گیرد تا تحت هیچ شرایطی پرش چیدمان (Layout Shift) رخ ندهد.
  - [x] از هرگونه `wrap_content` برای عرض یا ارتفاع کانتینر تصویر اکیداً پرهیز شود.

### گام ۴: لیست علاقه‌مندی‌ها (Favorites / Wishlist)
* **فایل‌های هدف:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/FavoritesAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/FavoritesAdapter.java)
  - [`android/app/src/main/res/layout/fragment_favorites.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/fragment_favorites.xml)
* **چک‌لیست اجرایی:**
  - [x] متد `onViewRecycled` در `FavoritesAdapter` پیاده‌سازی شده و تسک‌های در صف برای کارت‌های بازیافت‌شده لغو گردند.
  - [x] متن‌ها و قیمت‌های کالاهای ذخیره‌شده فوراً از دیتابیس محلی/کش رسم شوند و منتظر دانلود عکس‌های ریموت نمانند.
  - [x] تصویر موقت Shimmer به محض بایند شدن کارت نمایان باشد.

### گام ۵: سبد خرید (Cart Screen)
* **فایل‌های هدف:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/cart/CartAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/cart/CartAdapter.java)
  - [`android/app/src/main/res/layout/item_cart_line.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_cart_line.xml)
* **چک‌لیست اجرایی:**
  - [x] ابعاد تامبنیل تصویر در XML روی اندازه ثابت (مانند `80dp x 80dp`) با پس‌زمینه Shimmer قفل شود تا باز شدن صفحه هیچ پرشی نداشته باشد.
  - [x] قیمت واحد، قیمت کل سطر، هشدارهای تغییر قیمت و نام کالا در بالاترین اولویت متد `bind()` قرار گیرند.
  - [x] متد `onViewRecycled` برای لغو دانلود تصویر خطوط سبد خرید در هنگام اسکرول یا حذف پیاده‌سازی شود.

### گام ۶: بازبینی تسویه‌حساب و ثبت نهایی سفارش
* **فایل‌های هدف:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/checkout/ReviewItemsAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/checkout/ReviewItemsAdapter.java)
  - [`android/app/src/main/res/layout/item_review_line.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_review_line.xml)
* **چک‌لیست اجرایی:**
  - [x] رندر جزئیات فاکتور و لیست اقلام سفارش بدون هیچ مکث یا وابستگی به شبکه برای عکس‌ها انجام شود (<50ms).
  - [x] متد `onViewRecycled` برای تامبنیل‌های آیتم‌های سفارش ثبت گردد.
  - [x] در صورت خطای لود یا عدم اتصال اینترنت، فاکتور بدون مشکل نمایش داده شود و آیکون لوکس پیش‌فرض خطا جایگزین گردد.

### گام ۷: اسلایدر و گالری جزئیات محصول
* **فایل‌های هدف:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/ImageCarouselAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/ImageCarouselAdapter.java)
  - [`android/app/src/main/res/layout/item_product_image.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_product_image.xml)
  - [`android/app/src/main/res/layout/fragment_product_detail.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/fragment_product_detail.xml)
* **چک‌لیست اجرایی:**
  - [ ] تمامی مشخصات متنی (نام کالا، مشخصات فنی، برند، قیمت، وضعیت انبار) فوراً از پارامترهای ناوبری یا کش رسم شوند.
  - [ ] ویوپورت گالری در بالا دارای نسبت ثابت `4:5` یا `1:1` با پس‌زمینه Shimmer باشد.
  - [ ] متد `onViewRecycled` برای لغو عکس‌های بزرگ هنگام سوایپ سریع اسلایدر پیاده‌سازی شود.

---

## فاز ۳: بهینه‌سازی اسکرول و پیش‌بارگذاری اندروید (Android Prefetching)

### گام ۸: پیش‌واکشی فعال بر اساس جهت اسکرول
* **فایل‌های هدف:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/fragment/ExploreFragment.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/fragment/ExploreFragment.java)
  - [`android/app/src/main/java/com/averonlabs/eshopverse/shell/HomeFragment.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/shell/HomeFragment.java)
* **چک‌لیست اجرایی:**
  - [ ] اسکرول لیسنر کاتالوگ جهت اسکرول کاربر را تشخیص دهد.
  - [ ] در اسکرول به سمت پایین، آدرس ۴ تا ۶ تصویر آینده از طریق `ImageLoaderHelper.preloadBatch()` در حافظه کش قرار گیرد.
  - [ ] عملیات پیش‌بارگذاری با اولویت پایین (Low Priority) انجام شود تا پهنای باند آیتم‌های جاری داخل صفحه مصرف نشود.

---

## جدول پیگیری وضعیت اندروید

- [x] **گام ۱:** ارتقای متد پاکسازی منابع در `ImageLoaderHelper.java`
- [x] **گام ۲:** اعتبارسنجی تست‌های واحد در `ImageLoaderHelperTest.java`
- [x] **گام ۳:** پیاده‌سازی در `ProductGridAdapter.java` و لایه‌های گرید
- [x] **گام ۴:** پیاده‌سازی در `FavoritesAdapter.java`
- [x] **گام ۵:** پیاده‌سازی در `CartAdapter.java` و لایه سطر سبد خرید
- [ ] **گام ۶:** پیاده‌سازی در `ReviewItemsAdapter.java`
- [ ] **گام ۷:** پیاده‌سازی در `ImageCarouselAdapter.java` و لایه جزئیات
- [ ] **گام ۸:** پیاده‌سازی پیش‌بارگذاری فعال در فرگمنت‌ها
