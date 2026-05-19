# WebDAV Server Setup Guide

Notes App syncs via standard WebDAV. Here are tested configurations for popular servers.

## Quick Test Server

```bash
docker run -d -p 8080:80 -v $(pwd)/notes-sync:/var/lib/dav \
  -e USERNAME=notes -e PASSWORD=yourpassword \
  --name notes-dav morrisjobke/webdav
```

In the app: `http://your-server-ip:8080`, username `notes`, password `yourpassword`

---

## Nextcloud

Nextcloud has built-in WebDAV at `/remote.php/dav/files/USERNAME/`

**Setup:**
1. In Notes App, enable WebDAV sync
2. URL: `https://your-nextcloud.com/remote.php/dav/files/youruser/Notes/`
3. Username: your Nextcloud username
4. Password: **App password** (not your main password!)
   - Nextcloud → Settings → Security → Create app password

---

## Synology NAS

1. Enable WebDAV in DSM: Control Panel → File Services → WebDAV
2. Choose HTTP (port 5005) or HTTPS (port 5006)
3. In Notes App: `http://your-nas:5005/Notes/` or `https://your-nas:5006/Notes/`

---

## nginx with WebDAV

```nginx
server {
    listen 80;
    server_name notes-sync.local;
    root /var/www/dav;
    
    location / {
        dav_methods PUT DELETE MKCOL COPY MOVE;
        dav_ext_methods PROPFIND OPTIONS;
        create_full_put_path on;
        dav_access user:rw group:rw all:r;
        
        auth_basic "Notes Sync";
        auth_basic_user_file /etc/nginx/.htpasswd;
        
        autoindex on;
    }
}
```

Create password file:
```bash
htpasswd -c /etc/nginx/.htpasswd notesuser
```

---

## Apache mod_dav

```apache
<Directory "/var/www/dav">
    DAV On
    AuthType Basic
    AuthName "Notes Sync"
    AuthUserFile /etc/apache2/webdav.passwd
    Require valid-user
</Directory>
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Connection refused" | Check firewall, try HTTP first |
| "401 Unauthorized" | Use app password, not main account password |
| "SSL handshake failed" | Check certificate, allow self-signed in settings |
| Slow sync | Enable "Sync on WiFi only" in settings |
| Conflicts everywhere | Check server time is correct (NTP) |

## Security Notes

- Always use HTTPS for remote servers
- Generate app-specific passwords, never reuse main passwords
- Consider VPN for home server access instead of exposing WebDAV
- Enable "Local encryption" in app settings for sensitive notes
