# Open WebUI - AI Chat Interface

This project is **Open WebUI**, a self-hosted AI chat platform that provides a web interface for interacting with various language models like Ollama and OpenAI-compatible APIs.

## Project Overview

Open WebUI is a full-stack application with both frontend and backend components:

- **Frontend**: Modern web app built with SvelteKit and TypeScript
- **Backend**: Python FastAPI server with comprehensive AI integrations
- **Architecture**: Hybrid app with embedded backend or standalone deployment options

## Key Technologies

### Frontend Stack
- **Framework**: SvelteKit (TypeScript)
- **UI**: Tailwind CSS with custom components
- **Build**: Vite
- **Key Libraries**: 
  - TipTap (rich text editor)
  - Chart.js (visualizations)
  - Fuse.js (search)
  - i18next (internationalization)
  - Socket.io (real-time communication)

### Backend Stack
- **Framework**: FastAPI (Python 3.11+)
- **Database**: SQLAlchemy with multiple DB support (SQLite, PostgreSQL, MySQL)
- **Vector Databases**: ChromaDB, Qdrant, Milvus, Pinecone, etc.
- **AI/ML**: Transformers, Sentence Transformers, LangChain
- **Authentication**: OAuth, LDAP, JWT
- **File Processing**: PDF, DOCX, multimedia support

## Project Structure

```
open-webui/
├── backend/                 # Python FastAPI backend
│   └── open_webui/
│       ├── routers/        # API routes (chats, models, auth, etc.)
│       ├── models/         # Database models
│       ├── utils/          # Utilities (auth, chat, embeddings, etc.)
│       ├── retrieval/      # RAG and vector search
│       └── static/         # Static assets
├── src/                    # SvelteKit frontend
│   ├── lib/
│   │   ├── components/     # Svelte components
│   │   ├── apis/          # Frontend API clients
│   │   ├── stores/        # Svelte stores
│   │   ├── utils/         # Frontend utilities
│   │   └── i18n/          # Internationalization
│   └── routes/            # SvelteKit routes
├── static/                # Static assets
├── ChatDroid/             # Android companion app
└── docs/                  # Documentation
```

## Core Features

### Chat & Messaging
- Multi-model conversations
- Real-time streaming responses
- Message history and search
- File attachments and uploads
- Voice/video calling capabilities

### AI Model Integration
- **Ollama**: Local model support
- **OpenAI API**: GPT models
- **Custom Models**: Model builder and fine-tuning
- **Multiple Providers**: Anthropic, Google, etc.

### Document Processing & RAG
- **File Support**: PDF, DOCX, images, audio
- **RAG Pipeline**: Document indexing and retrieval
- **Vector Storage**: Multiple vector database backends
- **Web Search**: Integration with search providers

### Administration
- **User Management**: RBAC, groups, permissions
- **Model Management**: Install, configure, and manage models
- **System Settings**: Database, storage, integrations

## Development Commands

### Frontend Development
```bash
npm run dev              # Start dev server
npm run build           # Production build
npm run check           # Type checking
npm run lint            # Lint frontend and backend
npm run format          # Format code
```

### Backend Development
```bash
cd backend
python -m open_webui    # Start backend server
pip install -r requirements.txt
```

### Testing
```bash
npm run test:frontend   # Frontend tests
npm run cy:open         # Cypress E2E tests
```

## Environment & Configuration

### Key Environment Variables
- `OLLAMA_BASE_URL`: Ollama server URL
- `OPENAI_API_KEY`: OpenAI API key
- `DATABASE_URL`: Database connection string
- `WEBUI_SECRET_KEY`: JWT secret key

### Database Configuration
Supports multiple databases via SQLAlchemy:
- SQLite (default)
- PostgreSQL
- MySQL

### Storage Providers
- Local filesystem
- AWS S3
- Google Cloud Storage
- Azure Blob Storage

## API Architecture

### Backend Routes (`backend/open_webui/routers/`)
- `/api/v1/auths/` - Authentication
- `/api/v1/chats/` - Chat management
- `/api/v1/models/` - Model operations
- `/api/v1/files/` - File handling
- `/api/v1/users/` - User management
- `/api/v1/configs/` - System configuration

### Frontend API Layer (`src/lib/apis/`)
Organized by feature with TypeScript clients for each backend service.

## Special Considerations

### Security
- RBAC with granular permissions
- OAuth and LDAP integration
- API key management
- Content filtering and moderation

### Performance
- Vector search optimization
- Caching layers (Redis support)
- Streaming responses
- File upload handling

### Deployment
- Docker containers available
- Kubernetes manifests included
- Multiple deployment configurations
- Health checks and monitoring

## Testing Strategy

- **Frontend**: Vitest unit tests
- **Backend**: Pytest integration tests
- **E2E**: Cypress tests
- **API**: FastAPI test client

## Common Development Tasks

1. **Adding new features**: Typically involves both frontend components and backend API routes
2. **Database changes**: Use Alembic migrations in `backend/open_webui/migrations/`
3. **UI components**: Add to `src/lib/components/` with proper TypeScript types
4. **API integration**: Add to both backend routers and frontend API clients
5. **Internationalization**: Add translations to `src/lib/i18n/locales/`

## Build & Deployment

The project supports multiple deployment methods:
- **Docker**: Single container or docker-compose
- **Kubernetes**: Helm charts and manifests available  
- **Native**: Direct Python/Node.js installation
- **Hybrid**: Frontend built into backend static assets

## Code Quality

- ESLint + Prettier for frontend
- Black + Pylint for backend
- TypeScript strict mode
- Comprehensive testing suite

## Testing Environment and testing account

Web URL: 
http://34.121.157.227:3000 

Testing Account(Both for Web and Android):
email: zhenwei@gbox.ai
password: 123456
