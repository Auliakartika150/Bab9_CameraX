# ALUR IZIN :  
Saat pertama kali dijalankan, 
aplikasi meminta izin kamera dan penyimpanan. 
Logika permintaan izin terdapat di MainActivity.kt pada bagian ActivityResultContracts.RequestPermission. Jika izin ditolak, 
fitur kamera akan dinonaktifkan.

# MediaStore
Proses penyimpanan dilakukan dengan membuat ContentValues yang berisi metadata foto, 
lalu memanggil contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
contentValues) untuk mendapatkan URI penyimpanan, dan menulis foto ke OutputStream. 
Kode ini ada di fungsi takePhoto() di MainActivity.kt

# Rotasi Kamera
CameraX menangani rotasi perangkat dengan targetRotation. 
Sebelum menyimpan foto, aplikasi menyesuaikan orientasi agar foto tidak terbalik ketika perangkat diubah dari portrait ke landscape atau sebaliknya. 
Implementasinya dapat ditemukan di pengaturan imageCapture.targetRotation = viewFinder.display.rotation di MainActivity.kt
