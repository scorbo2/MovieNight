# MovieNight
A media organizing and playing application

## Interfaces
- `/` provides the public read-only media browser.
- `/admin` provides the localhost-only admin interface for create, edit, and delete actions.

## Admin authentication
- Admin access requires HTTP Basic authentication and must originate from localhost.
- Override the default credentials with `MOVIENIGHT_ADMIN_USERNAME` and `MOVIENIGHT_ADMIN_PASSWORD`.
