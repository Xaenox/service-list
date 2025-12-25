# GitHub Actions Workflows and Scripts

Этот каталог содержит конфигурацию GitHub Actions и скрипты для автоматизации CI/CD процессов.

## 📁 Структура

```
.github/
├── README.md                    # Этот файл
├── CLAUDE_CODE_REVIEW.md        # Документация по автоматическому code review
├── workflows/
│   ├── ci.yml                   # Основной CI workflow (тесты, сборка)
│   ├── security.yml             # Security scanning
│   ├── deploy.yml               # Deployment workflow
│   └── claude-review.yml        # Автоматический code review с Claude AI
└── scripts/
    ├── claude-review.py         # Скрипт для code review
    └── requirements.txt         # Python зависимости

```

## 🤖 Автоматический Code Review с Claude

Репозиторий настроен для автоматического ревью pull requests с помощью Claude AI.

### Быстрый старт

1. **Настройте GitHub Secret** (Settings → Secrets and variables → Actions):
   - `ANTHROPIC_API_KEY` - ваш API ключ от Anthropic

2. **Создайте Pull Request** - Claude автоматически проведет code review

3. **Получите результаты** - Claude опубликует детальный review как комментарий в PR

### Полная документация

См. [CLAUDE_CODE_REVIEW.md](./CLAUDE_CODE_REVIEW.md) для:
- Детальных инструкций по настройке
- Описания возможностей
- Troubleshooting
- Best practices
- Примеров использования

## 🔄 Workflows

### `ci.yml` - Continuous Integration

Запускается при push и pull request на main/master/develop ветки.

**Этапы:**
- Тесты backend (Kotlin + Ktor)
- Сборка приложения
- Code quality checks (ktlint)

### `claude-review.yml` - AI Code Review

Запускается при создании/обновлении pull request.

**Что делает:**
- Получает изменения из PR (git diff)
- Отправляет в Claude API для анализа
- Публикует детальный code review как комментарий
- Обновляет комментарий при новых коммитах

### `security.yml` - Security Scanning

Запускается при push на main ветку.

**Проверки:**
- Dependency scanning
- Code scanning
- Secret detection

### `deploy.yml` - Deployment

Запускается вручную или при push тегов.

**Этапы:**
- Build Docker image
- Push to registry
- Deploy to environment

## 🛠️ Скрипты

### `scripts/claude-review.py`

Автоматический code review с использованием Claude API.

**Использование:**
```bash
# Устанавливается автоматически в GitHub Actions workflow
python .github/scripts/claude-review.py
```

**Зависимости:**
```bash
pip install -r .github/scripts/requirements.txt
```

**Переменные окружения:**
- `ANTHROPIC_API_KEY` - API ключ Anthropic (required)
- `GITHUB_TOKEN` - GitHub token (автоматически доступен)
- `PR_NUMBER` - Номер pull request
- `REPO_NAME` - Имя репозитория
- `BASE_REF` - Base branch
- `HEAD_REF` - Head branch
- `CLAUDE_MODEL` - Модель Claude (optional, default: sonnet-4)

## 📝 Логи

Все workflows выводят подробные логи, которые можно просмотреть в:
**Actions → [выберите workflow] → [выберите run] → [выберите job]**

## 🔐 Безопасность

- Все секреты хранятся в GitHub Secrets (Settings → Secrets and variables → Actions)
- Secrets автоматически маскируются в логах
- Скрипты не логируют конфиденциальную информацию
- Fork PR требуют approval для запуска workflows

## 💡 Добавление новых workflows

Для добавления нового workflow:

1. Создайте файл в `.github/workflows/`
2. Следуйте [GitHub Actions синтаксису](https://docs.github.com/actions/reference/workflow-syntax-for-github-actions)
3. Добавьте необходимые секреты в Settings
4. Задокументируйте использование в этом README

## 📚 Дополнительные ресурсы

- [GitHub Actions Documentation](https://docs.github.com/actions)
- [Anthropic API Documentation](https://docs.anthropic.com/)
- [PyGithub Documentation](https://pygithub.readthedocs.io/)

---

Для вопросов и предложений создавайте issue в репозитории.
