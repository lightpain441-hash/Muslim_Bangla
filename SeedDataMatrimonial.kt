package com.example.data.local

object SeedDataMatrimonial {
    const val LOCAL_USER_ID = "local_user"

    fun starterProfiles(): List<MatrimonialProfileEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            MatrimonialProfileEntity(
                id = "profile_dh01",
                userId = "sample_patro_dh01",
                profileCode = "MB-DH01",
                gender = "PATRO",
                age = 27L,
                division = "ঢাকা",
                education = "বিএসসি ইন কম্পিউটার সায়েন্স (বুয়েট)",
                profession = "সফটওয়্যার ইঞ্জিনিয়ার",
                practicingNote = "পাঁচ ওয়াক্ত নামাজ, দাড়ি ও সুন্নাহসম্মত জীবনযাপন",
                bioBangla = "দ্বীনদার ও শালীন পরিবারের সন্ধানে আছি। পরিবারে পিতা-মাতা ও ছোট বোন রয়েছে। পর্দা ও হালাল উপার্জনের ব্যাপারে সচেতন।",
                waliRelation = "বাবা",
                photoUrl = null,
                photoBlurred = true,
                active = true,
                updatedAtMillis = now - 3_600_000L
            ),
            MatrimonialProfileEntity(
                id = "profile_dh02",
                userId = "sample_patri_dh02",
                profileCode = "MB-DH02",
                gender = "PATRI",
                age = 23L,
                division = "ঢাকা",
                education = "এমবিবিএস (ঢাকা মেডিকেল কলেজ)",
                profession = "ইন্টার্ন চিকিৎসক",
                practicingNote = "পূর্ণাঙ্গ হিজাব ও নিকাব, নিয়মিত কুরআন তিলাওয়াত",
                bioBangla = "ইসলামিক পরিবেশে বেড়ে ওঠা। দ্বীনদার, নম্র ও সৎ পাত্র প্রত্যাশী যিনি পরিবার ও দ্বীনের মধ্যে সুন্দর ভারসাম্য বজায় রাখবেন।",
                waliRelation = "বাবা",
                photoUrl = null,
                photoBlurred = true,
                active = true,
                updatedAtMillis = now - 7_200_000L
            ),
            MatrimonialProfileEntity(
                id = "profile_ch03",
                userId = "sample_patri_ch03",
                profileCode = "MB-CH03",
                gender = "PATRI",
                age = 24L,
                division = "চট্টগ্রাম",
                education = "এমএ ইন ইসলামিক স্টাডিজ (চট্টগ্রাম বিশ্ববিদ্যালয়)",
                profession = "কুরআন ও আরবি শিক্ষিকা",
                practicingNote = "পাঁচ ওয়াক্ত সালাত, পর্দা ও হাদিস অধ্যয়ন",
                bioBangla = "শান্ত ও সুশৃঙ্খল পারিবারিক পরিবেশ। ওয়ালীর সম্মতি ও শরিয়াহসম্মত আলোচনার মাধ্যমে বিবাহের ব্যাপারে আগ্রহী।",
                waliRelation = "বড় ভাই",
                photoUrl = null,
                photoBlurred = true,
                active = true,
                updatedAtMillis = now - 10_800_000L
            ),
            MatrimonialProfileEntity(
                id = "profile_sy04",
                userId = "sample_patro_sy04",
                profileCode = "MB-SY04",
                gender = "PATRO",
                age = 29L,
                division = "সিলেট",
                education = "এমবিএ (ফিন্যান্স), হাফেজে কুরআন",
                profession = "ইসলামী ব্যাংকিং কর্মকর্তা",
                practicingNote = "জামাতে নামাজ, সুন্নাহ আমল ও হালাল উপার্জন",
                bioBangla = "সুদমুক্ত ইসলামী আর্থিক প্রতিষ্ঠানে কর্মরত। পিতা অবসরপ্রাপ্ত শিক্ষক। দ্বীনদার ও পরহেজগার জীবনসঙ্গী খুঁজছি।",
                waliRelation = "বাবা",
                photoUrl = null,
                photoBlurred = true,
                active = true,
                updatedAtMillis = now - 14_400_000L
            ),
            MatrimonialProfileEntity(
                id = "profile_rj05",
                userId = "sample_patri_rj05",
                profileCode = "MB-RJ05",
                gender = "PATRI",
                age = 22L,
                division = "রাজশাহী",
                education = "বিবিএ (রাজশাহী বিশ্ববিদ্যালয়)",
                profession = "হোম টিউটর ও শিক্ষার্থিনী",
                practicingNote = "পর্দা, তাহাজ্জুদ ও দৈনন্দিন মাসনুন দুআ অনুশীলন",
                bioBangla = "সাদাসিধে ও ধার্মিক জীবনযাপন পছন্দ করি। অভিভাবকদের সরাসরি উপস্থিতিতে পারিবারিক পরিচিতি আদান-প্রদানে আগ্রহী।",
                waliRelation = "চাচা",
                photoUrl = null,
                photoBlurred = true,
                active = true,
                updatedAtMillis = now - 18_000_000L
            ),
            MatrimonialProfileEntity(
                id = "profile_kh06",
                userId = "sample_patro_kh06",
                profileCode = "MB-KH06",
                gender = "PATRO",
                age = 28L,
                division = "খুলনা",
                education = "বিএসসি ইন সিভিল ইঞ্জিনিয়ারিং (কুয়েট)",
                profession = "প্রজেক্ট ইঞ্জিনিয়ার",
                practicingNote = "পাঁচ ওয়াক্ত সালাত ও নিয়মিত ইসলামি সাহিত্য পাঠ",
                bioBangla = "সততা ও ইসলামিক মূল্যবোধকে সর্বোচ্চ অগ্রাধিকার দিই। নম্র ও দ্বীনদার জীবনসঙ্গিনী খুঁজছি।",
                waliRelation = "বাবা",
                photoUrl = null,
                photoBlurred = true,
                active = true,
                updatedAtMillis = now - 21_600_000L
            )
        )
    }

    fun starterIncomingRequest(): MatrimonialRequestEntity {
        val now = System.currentTimeMillis()
        return MatrimonialRequestEntity(
            id = "sample_patri_ch03_$LOCAL_USER_ID",
            senderId = "sample_patri_ch03",
            receiverId = LOCAL_USER_ID,
            senderName = "MB-CH03 (চট্টগ্রাম • শিক্ষিকা)",
            receiverName = "আমার প্রোফাইল",
            status = "PENDING",
            introMessage = "আসসালামু আলাইকুম। আমাদের পরিবারের পক্ষ থেকে অভিভাবকের মাধ্যমে আপনার বায়োডাটার ব্যাপারে কথা বলতে আগ্রহী।",
            photoRevealApproved = false,
            waliVerified = true,
            createdAtMillis = now - 1_800_000L,
            updatedAtMillis = now - 1_800_000L
        )
    }
}
