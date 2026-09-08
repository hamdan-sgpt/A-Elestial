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
