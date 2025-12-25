#!/usr/bin/env python3
"""
Автоматический code review для GitHub Pull Requests с использованием Claude API
"""
import os
import sys
import subprocess
from anthropic import Anthropic
from github import Github


def get_env_variable(name, required=True):
    """Получить переменную окружения"""
    value = os.environ.get(name)
    if required and not value:
        print(f"❌ Ошибка: переменная окружения {name} не установлена")
        sys.exit(1)
    return value


def get_pull_request_diff():
    """Получить изменения в pull request используя git diff"""
    try:
        base_ref = get_env_variable('BASE_REF')
        head_ref = get_env_variable('HEAD_REF')

        # Получаем diff между base и head
        result = subprocess.run(
            ['git', 'diff', f'origin/{base_ref}...HEAD'],
            capture_output=True,
            text=True,
            check=True
        )

        diff = result.stdout

        if not diff.strip():
            print("ℹ️  Нет изменений для ревью")
            return None

        return diff

    except subprocess.CalledProcessError as e:
        print(f"❌ Ошибка при получении git diff: {e}")
        print(f"Stderr: {e.stderr}")
        sys.exit(1)


def get_file_changes_summary():
    """Получить краткую сводку измененных файлов"""
    try:
        base_ref = get_env_variable('BASE_REF')

        result = subprocess.run(
            ['git', 'diff', '--stat', f'origin/{base_ref}...HEAD'],
            capture_output=True,
            text=True,
            check=True
        )

        return result.stdout
    except subprocess.CalledProcessError:
        return "Не удалось получить статистику изменений"


def analyze_with_claude(diff, pr_info, files_stat):
    """Анализировать изменения с помощью Claude API"""
    api_key = get_env_variable('ANTHROPIC_API_KEY')
    client = Anthropic(api_key=api_key)

    # Формируем промпт для Claude
    prompt = f"""Вы - опытный code reviewer. Проведите детальный анализ следующих изменений в pull request.

**Информация о Pull Request:**
- Заголовок: {pr_info['title']}
- Описание: {pr_info.get('body', 'Не указано') or 'Не указано'}
- Автор: {pr_info['author']}
- Base Branch: {pr_info['base_ref']}
- Head Branch: {pr_info['head_ref']}

**Измененные файлы:**
```
{files_stat}
```

**Изменения (git diff):**
```diff
{diff[:15000]}
```
{"..." if len(diff) > 15000 else ""}

Пожалуйста, проведите code review и предоставьте:

1. **Общая оценка** - краткая оценка качества изменений (1-2 предложения)

2. **Положительные моменты** - что сделано хорошо

3. **Проблемы и замечания** (если есть):
   - 🔴 Критичные проблемы (security, bugs, breaking changes)
   - 🟡 Важные замечания (code quality, best practices)
   - 🔵 Предложения по улучшению (optional improvements)

4. **Рекомендации** - конкретные действия для улучшения кода

5. **Вывод** - можно ли мержить (✅ Можно мержить / ⚠️ С замечаниями / ❌ Требуются исправления)

Форматируйте ответ в Markdown для удобного отображения в GitHub.
Будьте конструктивными и конкретными в замечаниях.

Учитывайте специфику проекта:
- Backend: Kotlin + Ktor framework
- Frontend: React + TypeScript
- Database: PostgreSQL
- Обращайте внимание на security (OAuth2, JWT, SQL injection, XSS)
"""

    try:
        print("🤖 Отправка запроса в Claude API...")

        # Выбираем модель
        model = get_env_variable('CLAUDE_MODEL', required=False) or 'claude-sonnet-4-20250514'

        response = client.messages.create(
            model=model,
            max_tokens=4096,
            temperature=0.3,
            messages=[{
                "role": "user",
                "content": prompt
            }]
        )

        review_text = response.content[0].text
        print("✅ Code review получен от Claude")

        return review_text

    except Exception as e:
        print(f"❌ Ошибка при вызове Claude API: {e}")
        sys.exit(1)


def post_review_to_pull_request(review_text, pr_info):
    """Опубликовать результаты ревью как комментарий в PR"""
    try:
        github_token = get_env_variable('GITHUB_TOKEN')
        repo_name = get_env_variable('REPO_NAME')
        pr_number = int(get_env_variable('PR_NUMBER'))

        # Подключаемся к GitHub
        g = Github(github_token)
        repo = g.get_repo(repo_name)
        pr = repo.get_pull(pr_number)

        # Проверяем, есть ли уже комментарий от бота
        bot_comment = None
        for comment in pr.get_issue_comments():
            if comment.user.login == g.get_user().login and "🤖 Автоматический Code Review от Claude" in comment.body:
                bot_comment = comment
                break

        # Формируем комментарий
        model = get_env_variable('CLAUDE_MODEL', required=False) or 'claude-sonnet-4-20250514'
        comment_body = f"""## 🤖 Автоматический Code Review от Claude

{review_text}

---
*Этот review сгенерирован автоматически с помощью Claude API*
*Модель: `{model}`*
*Обновлено: {pr_info['updated_at']}*
"""

        # Создаем или обновляем комментарий
        if bot_comment:
            bot_comment.edit(comment_body)
            print(f"✅ Code review обновлен в PR #{pr_number}")
        else:
            pr.create_issue_comment(comment_body)
            print(f"✅ Code review опубликован в PR #{pr_number}")

        return True

    except Exception as e:
        print(f"❌ Ошибка при публикации комментария в GitHub: {e}")
        # Не фейлим workflow, просто выводим review в лог
        print("\n" + "="*80)
        print("CODE REVIEW:")
        print("="*80)
        print(review_text)
        print("="*80)
        return False


def main():
    """Основная функция"""
    print("🚀 Запуск автоматического code review с Claude...")

    # Получаем информацию о PR из переменных окружения GitHub Actions
    pr_info = {
        'title': get_env_variable('PR_TITLE'),
        'body': get_env_variable('PR_BODY', required=False),
        'author': get_env_variable('PR_AUTHOR'),
        'base_ref': get_env_variable('BASE_REF'),
        'head_ref': get_env_variable('HEAD_REF'),
        'number': get_env_variable('PR_NUMBER'),
        'updated_at': subprocess.run(
            ['date', '-u', '+%Y-%m-%d %H:%M:%S UTC'],
            capture_output=True,
            text=True,
            check=True
        ).stdout.strip()
    }

    print(f"📋 Анализируем PR #{pr_info['number']}: {pr_info['title']}")
    print(f"   {pr_info['head_ref']} → {pr_info['base_ref']}")

    # Получаем изменения
    diff = get_pull_request_diff()
    if not diff:
        print("ℹ️  Нет изменений для анализа")
        sys.exit(0)

    print(f"📊 Размер diff: {len(diff)} символов")

    # Получаем статистику файлов
    files_stat = get_file_changes_summary()

    # Анализируем с помощью Claude
    review = analyze_with_claude(diff, pr_info, files_stat)

    # Публикуем результаты
    success = post_review_to_pull_request(review, pr_info)

    if success:
        print("✅ Code review завершен успешно!")
    else:
        print("⚠️  Code review выполнен, но не опубликован в PR (см. логи выше)")
        sys.exit(0)  # Не фейлим workflow


if __name__ == '__main__':
    main()
