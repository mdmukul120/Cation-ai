# CapGrok - AI Voice Analysis & Synchronized Caption Video Editor

গুগল জেমিনি (Google Gemini) এবং এক্সএআই গর্ক (xAI Grok) এপিআই দ্বারা চালিত একটি আধুনিক অ্যান্ড্রয়েড ভিডিও ও সাবটাইটেল এডিটর অ্যাপ। এটি যেকোনো অডিও বিশ্লেষণ করে স্বয়ংক্রিয়ভাবে বাংলা ও ইংরেজিতে সামারি প্রস্তুত করে এবং ক্যাপকাট-স্টাইলে অ্যানিমেটেড সাবটাইটেল সহ দ্রুত MP4 ভিডিও তৈরি করে।

---

## 🚀 গিটহাবে পুশ ও অটোমেটিক রিলিজ সেটআপ (GitHub Push & Auto Release)

এই রিপোজিটরিতে **GitHub Actions CI/CD** সম্পূর্ণ প্রস্তুত করা আছে (`.github/workflows/release.yml`)। আপনি কোড গিটহাবে পুশ করলেই স্বয়ংক্রিয়ভাবে APK ও AAB ফাইল বিল্ড হয়ে **GitHub Releases** ট্যাবে যুক্ত হবে।

### ধাপ ১: গিট রিপোজিটরি তৈরি ও পুশ করা (Git Push Commands)
আপনার কম্পিউটারের টার্মিনালে নিচের কমান্ডগুলো চালান:

```bash
# ১. গিট ইনিশিয়ালাইজ করুন (যদি পূর্বে না করা থাকে)
git init

# ২. মেইন ব্রাঞ্চ নির্বাচন করুন
git branch -M main

# ৩. সব ফাইল যুক্ত ও কমিট করুন
git add .
git commit -m "feat: CapGrok AI Video Caption Editor with Grok & Gemini"

# ৪. আপনার GitHub রিপোজিটরি লিঙ্ক করুন (YOUR_USERNAME এবং REPO_NAME পরিবর্তন করুন)
git remote add origin https://github.com/YOUR_USERNAME/CapGrok.git

# ৫. গিটহাবে পুশ করুন
git push -u origin main
```

---

### ধাপ ২: নতুন ভার্সন ট্যাগ দিয়ে অটো রিলিজ তৈরি করা (Auto Release with Tags)
যখনই আপনি একটি নতুন রিলিজ তৈরি করতে চান, শুধু একটি ট্যাগ পুশ করুন:

```bash
git tag v1.0.0
git push origin v1.0.0
```

ট্যাগ পুশ করার সাথে সাথে **GitHub Actions** স্বয়ংক্রিয়ভাবে:
1. কোড টেস্ট করবে (`./gradlew testDebugUnitTest`)
2. সাইনড **Release APK**, **Debug APK** এবং **Google Play Bundle (AAB)** তৈরি করবে।
3. আপনার গিটহাব রিপোজিটরির **Releases** সেকশনে সরাসরি ডাউনলোড লিংক সহ নতুন রিলিজ পাবলিশ করবে!

---

### ধাপ ৩: গিটহাব সিক্রেটস (GitHub Secrets - ঐচ্ছিক)
যদি আপনি চান যে অ্যাপের ভেতরে Gemini বা Grok এপিআই কী স্বয়ংক্রিয়ভাবে যুক্ত থাকুক, তবে আপনার GitHub রিপোজিটরির:
**Settings > Secrets and variables > Actions > New repository secret** এ গিয়ে নিচের কীগুলো যুক্ত করতে পারেন:
- `GEMINI_API_KEY`: আপনার Google Gemini API Key
- `GROK_API_KEY`: আপনার xAI Grok API Key

*(যদি না-ও দেন, অ্যাপটি কোনো ত্রুটি ছাড়াই বিল্ড হবে এবং ব্যবহারকারীরা অ্যাপের ভেতর সেটিংস ডায়ালগ থেকে যেকোনো সময় এপিআই কী দিতে পারবেন।)*

---

## 📱 প্রধান ফিচারসমূহ
- **Grok Voice Analysis:** ভয়েস ক্যাডেন্স, কথার গতি (WPM), আবেগ এবং পাঞ্চ কী-ওয়ার্ড বিশ্লেষণ।
- **Gemini Subtitles & Summary:** বাংলা ও ইংরেজিতে সারসংক্ষেপ এবং মিলিসেকেন্ড নিখুঁত সাবটাইটেল সিঙ্ক।
- **CapCut-Style Templates:** Hormozi Punch, CapCut Bounce, Minimal Pill, Karaoke Flow, Neon Cyber, Cinematic।
- **Fast Video Rendering:** অ্যান্ড্রয়েড নেটিভ হার্ডওয়্যার MediaCodec ও MediaMuxer দিয়ে তাৎক্ষণিক MP4 এক্সপোর্ট।
- **FFmpeg & SRT Exporter:** কমান্ড লাইন ব্যবহারকারীদের জন্য প্রস্তুত FFmpeg স্ক্রিপ্ট ও SRT ফাইল এক্সপোর্ট।
