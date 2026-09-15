Local frontend (Next.js 14 App Router)

Commands

Install dependencies:

npm install

Run dev server:

npm run dev

Notes

- Set `NEXT_PUBLIC_API_BASE_URL` in `.env.local` (see `.env.local.example`). Defaults to `http://localhost:8080`.
- The Spring Boot backend must allow CORS from `http://localhost:3000` for the Next.js dev server.
