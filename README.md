# jdmatchr-core
🧠 JDMatchR Core – The secure, modular Spring Boot backend powering JDMatchR. Includes OAuth2 login, GPT-4o-based resume-JD analysis, and RESTful APIs for full-stack integration.


# 🧠 JDMatchR Core – Spring Boot Backend

**JDMatchR Core** is the secure and extensible Spring Boot backend that powers [JDMatchR](https://github.com/ayzeysdev/jdmatchr-ui) — a resume and job description analyzer that helps job seekers align their resumes to real job posts.

This backend provides:
- ✅ Secure authentication (OAuth2 + JWT)
- 🔐 Role-based access control
- 🧠 GPT-4o integration via OpenAI
- 📊 REST APIs for resume parsing, JD analysis, and match scoring
- 🚀 Docker-ready + CI/CD friendly

---

## ⚙️ Tech Stack

| Component        | Technology                               |
|------------------|-------------------------------------------|
| Language         | Java 17                                   |
| Framework        | Spring Boot 3.x                           |
| Auth             | Spring Security + OAuth2 (Google, GitHub) |
| AI Integration   | OpenAI GPT-4o API                         |
| REST API         | Spring Web + Controller Advice            |
| Docs             | SpringDoc / Swagger                       |
| Packaging        | Docker                                    |

---

## 🔐 Authentication

- OAuth2 Login via Google & GitHub
- JWT issued after OAuth flow for frontend session
- Custom `SecurityConfig` to protect endpoints
- Role-based endpoint access (`/analyze`, `/admin`, etc.)

---

## 🧠 GPT-4o Integration

**OpenAI GPT-4o** is used for:
- Resume analysis
- JD keyword extraction
- Match scoring
- Text improvement suggestions

**Endpoints** (via `OpenAIService`):
- `POST /analyze/resume`
- `POST /analyze/jd`
- `POST /match`
- `POST /suggest`

You can configure prompts in `src/main/resources/prompts/`.
