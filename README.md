# SIMPATIK

**S**istem **M**anajemen **P**enilaian **A**nalitik **T**enaga kerja **I**ntegrated ter**K**omputerisasi

Aplikasi desktop untuk menilai dan meranking karyawan memakai metode
**SAW (Simple Additive Weighting)**. Dibangun dengan Java Swing, NetBeans,
dan MySQL.

## Kebutuhan

- JDK 25
- NetBeans IDE (project Ant, `build.xml`) — atau Apache Ant
- MySQL/MariaDB (dites dengan XAMPP/LAMPP, port 3306)
- Dependency jar: folder `~/Documents/Java/Library` dan
  `~/Documents/Java/Library/jaspersoft yt` (lihat `nbproject/project.properties`)

## Setup Database

```sql
CREATE DATABASE simpatik;
```

Tabel yang dibutuhkan:

| Tabel | Dipakai oleh |
|---|---|
| `users` | login (`auth.form_login`) |
| `karyawan` | `form_karyawan` |
| `kriteria` | `form_kriteria`, `form_prosesData` |
| `penilaian` | `form_penilaian`, `form_prosesData` |
| `hasil_ranking` | `form_prosesData`, laporan perangkingan |

Kolom penting:

- `kriteria(id, nama_kriteria, jenis_kriteria, bobot)` — **harus tepat 10 baris**,
  `jenis_kriteria` = `Benefit` | `Cost`, total `bobot` = 1.0
- `penilaian(kode_karyawan, c1 … c10)` — nilai per karyawan, sejajar urutan kriteria
- `hasil_ranking(kode_karyawan, nilai_preferensi, rank_position)` — **tabel turunan**,
  di-DELETE + INSERT ulang setiap kali tombol RANK ditekan

> Kredensial koneksi ada di `src/config/Koneksi.java`
> (`localhost:3306/simpatik`, user `root`, password kosong).
> Login default: `admin` / `admin` — ganti sebelum dipakai serius.

> Belum ada file `.sql` di repo ini. Dump schema + seed perlu ditambahkan.

## Build & Jalankan

Dari NetBeans: buka project, Run.

Dari terminal:

```bash
/usr/lib/netbeans/extide/ant/bin/ant clean jar
java -jar dist/SIMPATIK.jar
```

## Alur Kerja

1. **Master → Data Karyawan** — CRUD karyawan
2. **Master → Data Kriteria** — 10 kriteria + bobot + jenis (Benefit/Cost)
3. **Perhitungan → Penilaian** — isi nilai `c1…c10` per karyawan
4. **Perhitungan → Proses Data** — `NORMALISASI` lalu `RANK`
5. **Perhitungan → Report** — cetak 4 laporan JasperReports

### Rumus SAW

- Benefit: `r_ij = x_ij / max(x_j)`
- Cost: `r_ij = min(x_j) / x_ij`
- `V_i = Σ (r_ij × w_j)` → diurutkan menaik, rank 1 = terbaik

## Struktur

```
src/
├── auth/form_login.java      # main class
├── config/Koneksi.java       # koneksi JDBC singleton
├── ireport/*.jasper          # 4 laporan
├── img/                      # ikon
└── main/
    ├── Menu_Utama.java       # frame utama + sidebar
    ├── MenuItem.java
    ├── dashboard.java
    ├── form_karyawan.java
    ├── form_kriteria.java
    ├── form_penilaian.java
    ├── form_prosesData.java  # normalisasi + ranking SAW
    └── form_kriteria1.java   # halaman laporan
```

## Keterbatasan Diketahui

- Password disimpan plaintext di `users`
- Laporan dibaca dari path relatif `src/ireport/` → hanya jalan kalau CWD = folder
  project (dari NetBeans). Dari `dist/SIMPATIK.jar` gagal.
- Ada dua versi JasperReports di classpath (7.0.7 dan 6.20.6); 7.0.7 yang dipakai.
- Animasi sidebar `MenuItem` menyentuh component Swing dari background thread.
- `form_kriteria1` salinan `form_kriteria` dengan isi body dikomentari.
