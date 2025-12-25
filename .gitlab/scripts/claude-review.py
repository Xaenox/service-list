#!/usr/bin/env python3
"""
Автоматический code review для GitLab Merge Requests с использованием Claude API
"""
import os
import sys
import json
import subprocess
from anthropic import Anthropic
import gitlab


def get_env_variable(name, required=True):
    """Получить переменную окружения"""
    value = os.environ.get(name)
    if required and not value:
        print(f"❌ Ошибка: переменная окружения {name} не установлена")
        sys.exit(1)
    return value


def get_merge_request_changes():
    """Получить изменения в merge request используя git diff"""
    try:
        # Получаем изменения между target branch и текущим branch
        target_branch = get_env_variable('CI_MERGE_REQUEST_TARGET_BRANCH_NAME')
        source_branch = get_env_variable('CI_MERGE_REQUEST_SOURCE_BRANCH_NAME')

        # Fetch target branch для корректного diff
        subprocess.run(['git', 'fetch', 'origin', target_branch], check=True, capture_output=True)

        # Получаем diff
        result = subprocess.run(
            ['git', 'diff', f'origin/{target_branch}...HEAD'],
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


def analyze_with_claude(diff, mr_info):
    """Анализировать изменения с помощью Claude API"""
    api_key = get_env_variable('ANTHROPIC_API_KEY')
    client = Anthropic(api_key=api_key)

    # Формируем промпт для Claude
    prompt = f"""Вы - опытный code reviewer. Проведите детальный анализ следующих изменений в merge request.

**Информация о Merge Request:**
- Заголовок: {mr_info['title']}
- Описание: {mr_info.get('description', 'Не указано')}
- Автор: {mr_info['author']}
- Source Branch: {mr_info['source_branch']}
- Target Branch: {mr_info['target_branch']}

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

Форматируйте ответ в Markdown для удобного отображения в GitLab.
Будьте конструктивными и конкретными в замечаниях, указывайте номера строк где возможно.
"""

    try:
        print("🤖 Отправка запроса в Claude API...")

        # Выбираем модель в зависимости от переменной окружения
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


def post_review_to_merge_request(review_text):
    """Опубликовать результаты ревью как комментарий в MR"""
    try:
        gitlab_token = get_env_variable('GITLAB_TOKEN')
        gitlab_url = get_env_variable('CI_SERVER_URL')
        project_id = get_env_variable('CI_PROJECT_ID')
        mr_iid = get_env_variable('CI_MERGE_REQUEST_IID')

        # Подключаемся к GitLab
        gl = gitlab.Gitlab(gitlab_url, private_token=gitlab_token)
        project = gl.projects.get(project_id)
        mr = project.mergerequests.get(mr_iid)

        # Формируем комментарий
        comment = f"""## 🤖 Автоматический Code Review от Claude

{review_text}

---
*Этот review сгенерирован автоматически с помощью Claude API*
*Модель: {get_env_variable('CLAUDE_MODEL', required=False) or 'claude-sonnet-4-20250514'}*
"""

        # Публикуем комментарий
        mr.notes.create({'body': comment})
        print(f"✅ Code review опубликован в MR !{mr_iid}")

    except Exception as e:
        print(f"❌ Ошибка при публикации комментария в GitLab: {e}")
        # Не фейлим пайплайн, просто выводим review в лог
        print("\n" + "="*80)
        print("CODE REVIEW:")
        print("="*80)
        print(review_text)
        print("="*80)


def main():
    """Основная функция"""
    print("🚀 Запуск автоматического code review с Claude...")

    # Проверяем, что мы в контексте merge request
    if not os.environ.get('CI_MERGE_REQUEST_IID'):
        print("ℹ️  Не merge request pipeline, пропускаем code review")
        sys.exit(0)

    # Получаем информацию о MR из переменных окружения GitLab CI
    mr_info = {
        'title': get_env_variable('CI_MERGE_REQUEST_TITLE'),
        'description': get_env_variable('CI_MERGE_REQUEST_DESCRIPTION', required=False) or '',
        'author': get_env_variable('GITLAB_USER_LOGIN'),
        'source_branch': get_env_variable('CI_MERGE_REQUEST_SOURCE_BRANCH_NAME'),
        'target_branch': get_env_variable('CI_MERGE_REQUEST_TARGET_BRANCH_NAME'),
        'iid': get_env_variable('CI_MERGE_REQUEST_IID')
    }

    print(f"📋 Анализируем MR !{mr_info['iid']}: {mr_info['title']}")
    print(f"   {mr_info['source_branch']} → {mr_info['target_branch']}")

    # Получаем изменения
    diff = get_merge_request_changes()
    if not diff:
        sys.exit(0)

    print(f"📊 Размер diff: {len(diff)} символов")

    # Анализируем с помощью Claude
    review = analyze_with_claude(diff, mr_info)

    # Публикуем результаты
    post_review_to_merge_request(review)

    print("✅ Code review завершен успешно!")


if __name__ == '__main__':
    main()
