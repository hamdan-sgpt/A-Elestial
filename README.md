# 👻 Elestial Jumpscare Mod

Mod untuk ngagetin teman / player lain di Minecraft pake gambar & suara custom via link!

---

## 🎮 Cara Menggunakan

1. **Buka Menu Jumpscare**: Tekan tombol **`J`** di keyboard pas di dalam game (atau ketik `/jumpscare` di chat).
2. **Kirim via Chat Prefix**:
   - `!jumpscare <nama_player> <id_jumpscare>` *(contoh: `!jumpscare Steve spooky`)*
   - `#jumpscare <nama_player> <id_jumpscare>`

---

## 🌐 Cara Tambah Jumpscare Baru (Via Link URL)

Kamu bisa tambah jumpscare baru langsung dari game menggunakan link gambar & suara (tidak perlu folder/restart game)!

### Cara 1: Lewat Menu In-Game (`J`)
1. Tekan tombol **`J`** lalu pilih tab **`[ CUSTOM ]`**.
2. Masukkan:
   - **ID Jumpscare**: Nama ID baru *(contoh: `hantu`, `kunti`)*
   - **Link Gambar**: URL gambar (`https://...png`, `jpg`, `gif`)
   - **Link Suara**: URL suara (`https://...wav`, `mp3`, `ogg`)
3. Klik **`SIMPAN SERVER`** (bisa juga klik **`PASTE LINK`** buat otomatis nempel link dari clipboard).

### Cara 2: Lewat Command Chat
Ketik perintah berikut di chat:
```text
/jumpscare add <id> <link_gambar> <link_suara>
```
*(Contoh: `/jumpscare add kunti https://example.com/kunti.png https://example.com/kunti.mp3`)*

---

## 👻 Command Team "Ghost"

Fitur untuk memasukkan atau mengeluarkan player dari team scoreboard `"Ghost"`:

### 1. Via Command Brigadier
- `/ghost <nama_player>` *(contoh: `/ghost Steve` atau `/ghost @a`)*: Masukkan player ke team Ghost.
- `/ghost`: Masukkan diri sendiri ke team Ghost.
- `/ghost add <nama_player>`: Masukkan player ke team Ghost.
- `/ghost remove <nama_player>`: Keluarkan player dari team Ghost.
- `/ghost leave`: Keluar dari team Ghost.
- `/ghost list`: Lihat daftar semua pemain di team Ghost.
- `/ghost clear`: Kosongkan seluruh anggota team Ghost.
- `/unghost <nama_player>`: Alias cepat mengeluarkan player dari team Ghost.

### 2. Via Chat Prefix
- `!ghost <nama_player>` atau `#ghost <nama_player>` atau `/ghost <nama_player>`
- `!unghost <nama_player>` atau `#unghost <nama_player>` atau `/unghost <nama_player>`

### 3. Via Menu In-Game (`J`)
- Buka menu dengan tombol **`J`**, pilih tab **`[ KONTROL ]`**.
- Pilih pemain target, lalu klik tombol **`👻 + Team Ghost`** atau **`❌ - Team Ghost`**.

### 4. ⚔️ Mekanik Pasif: Ghost Attack → Jumpscare
- Setiap player yang berada di **team Ghost** jika memukul/menyerang player lain **di luar team Ghost** (korban), korban akan **otomatis langsung terkena efek Jumpscare** (gambar + audio mengagetkan).
- **Pilihan Jumpscare Khusus Ghost:**
  - Setiap Ghost bisa memilih jumpscare mana yang keluar saat memukul!
  - **Via Menu GUI (`J`)**: Pilih ID jumpscare yang diinginkan di Tab Visual, lalu klik **`[ 👻 Serangan Ghost: <id> ]`** atau klik **`[ 🎲 Random ]`**.
  - **Via Command**: `/ghost select <id>` *(contoh: `/ghost select kunti`)* atau `/ghost select random`.
  - **Cek Jumpscare Aktif**: `/ghost select` (tanpa argumen).
  - **Via Chat Prefix**: `!ghost select <id>` atau `!ghost set <id>`.
- **Anti-Spam Cooldown:** Diberi cooldown ~2.5 detik per korban agar tidak tumpang tindih bila dipukul bertubi-tubi.
- **Sesama Ghost:** Jika sesama player di team Ghost saling pukul, jumpscare **tidak** akan aktif.

### 5. 🛡️ Mekanik Eliminasi: Non-Ghost Serang Ghost → Auto /gmsp (Spectator)
- Jika player biasa (**di luar team Ghost**) menyerang player yang berada di **team Ghost**:
  - Player Ghost yang terkena serangan tersebut **otomatis langsung diubah mode permainannya menjadi Spectator (`/gmsp`)** seolah-olah hantunya berhasil tertangkap/tereliminasi.
  - Serangan fisik dibatalkan sehingga Ghost tidak mati/drop inventory ke tanah.
  - Server otomatis mengirimkan notifikasi chat ke penyerang, korban, dan broadcast ke seluruh player server!

---

## 👥 Pengujian Multi-Player (Multi-Client Test)

Untuk mengetes jumpscare, audio 3D, atau possession ke banyak player sekaligus di satu PC:
- **Jalankan 4 Client Sekaligus**: Double-click file [`run_4_clients.bat`](file:///d:/codingan/A-Elestial/run_4_clients.bat)
  - **Client 1**: Host / Dev (membuka dunia via "Open to LAN")
  - **Client 2**: `Victim_Test`
  - **Client 3**: `Victim_Test2`
  - **Client 4**: `Victim_Test3`
- **Jalankan 5 Client Sekaligus**: Double-click file [`run_5_clients.bat`](file:///d:/codingan/A-Elestial/run_5_clients.bat)
  - **Client 1**: Host / Dev
  - **Client 2**: `Victim_Test`
  - **Client 3**: `Victim_Test2`
  - **Client 4**: `Victim_Test3`
  - **Client 5**: `Victim_Test4`
- Client 2, 3, 4, dan 5 dapat langsung join ke LAN Server dari Client 1 untuk uji coba tembak massal!
