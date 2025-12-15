# Git 工作流程指南

本文档整理了日常开发中常用的 Git 命令，用于将代码提交到 fork 仓库。

---

## 仓库配置

| 远程名 | 地址 | 用途 |
|--------|------|------|
| `origin` | `git@github.com:rive-app/rive-android.git` | Rive 官方仓库（只拉取） |
| `fork` | `git@github.com:byzyhm/rive-android.git` | 个人 Fork 仓库（推送） |

```bash
# 查看远程仓库配置
git remote -v
```

---

## 日常提交流程

### 1. 查看当前修改状态

```bash
git status
```

### 2. 添加文件到暂存区

```bash
# 添加单个文件
git add <文件路径>

# 添加所有修改的文件
git add .

# 添加所有文件（包括删除的）
git add -A
```

### 3. 提交到本地仓库

```bash
git commit -m "feat: 你的提交信息"
```

### 4. 推送到 Fork 仓库

```bash
git push fork master
```

### 完整示例

```bash
# 查看状态
git status

# 添加所有修改
git add .

# 提交
git commit -m "feat: Add onboarding animation support"

# 推送到 fork
git push fork master
```

### 一键提交并推送

```bash
git add . && git commit -m "feat: 描述" && git push fork master
```

---

## 从官方仓库同步更新

当 Rive 官方仓库有新的更新时，使用以下命令同步：

```bash
# 1. 拉取官方最新代码
git fetch origin

# 2. 合并到本地
git merge origin/master

# 3. 如果有冲突，解决后：
git add .
git commit -m "merge: Sync with upstream"

# 4. 推送到你的 fork
git push fork master
```

---

## 常用查看命令

### 查看提交历史

```bash
# 查看最近 10 条提交
git log --oneline -10

# 查看详细提交历史
git log

# 查看某个文件的提交历史
git log --oneline <文件路径>
```

### 查看本地与远程的差异

```bash
# 本地比 fork 多的提交
git log --oneline fork/master..HEAD

# 本地比官方多的提交
git log --oneline origin/master..HEAD

# 查看要推送的文件统计
git diff --stat fork/master..HEAD
```

### 查看文件修改详情

```bash
# 查看未暂存的修改
git diff

# 查看已暂存的修改
git diff --staged

# 查看某个文件的修改
git diff <文件路径>
```

---

## 撤销操作

### 撤销工作区的修改（未 add）

```bash
# 撤销单个文件
git checkout -- <文件>

# 撤销所有修改
git checkout -- .
```

### 撤销暂存区（已 add 但未 commit）

```bash
# 撤销单个文件的暂存
git reset HEAD <文件>

# 撤销所有暂存
git reset HEAD
```

### 撤销提交

```bash
# 撤销最后一次 commit（保留修改在工作区）
git reset --soft HEAD~1

# 撤销最后一次 commit（保留修改在暂存区）
git reset --mixed HEAD~1

# 撤销最后一次 commit（丢弃所有修改）⚠️ 谨慎使用
git reset --hard HEAD~1
```

### 修改最后一次提交

```bash
# 修改提交信息
git commit --amend -m "新的提交信息"

# 追加文件到最后一次提交（不改变提交信息）
git add <遗漏的文件>
git commit --amend --no-edit
```

---

## 分支操作

### 创建与切换分支

```bash
# 创建新分支
git branch <分支名>

# 切换分支
git checkout <分支名>

# 创建并切换分支（一步完成）
git checkout -b <分支名>

# 查看所有分支
git branch -a
```

### 合并分支

```bash
# 切换到目标分支
git checkout master

# 合并其他分支
git merge <分支名>
```

### 删除分支

```bash
# 删除本地分支
git branch -d <分支名>

# 强制删除本地分支
git branch -D <分支名>
```

---

## 暂存工作区（Stash）

当需要临时切换分支但不想提交当前修改时：

```bash
# 暂存当前修改
git stash

# 暂存并添加描述
git stash save "描述信息"

# 查看暂存列表
git stash list

# 恢复最近的暂存
git stash pop

# 恢复指定的暂存
git stash apply stash@{0}

# 删除暂存
git stash drop stash@{0}

# 清空所有暂存
git stash clear
```

---

## 提交信息规范

推荐使用以下前缀来规范提交信息：

| 前缀 | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: Add onboarding animation` |
| `fix` | Bug 修复 | `fix: Fix text not appearing` |
| `docs` | 文档更新 | `docs: Update README` |
| `style` | 代码格式（不影响功能） | `style: Format code` |
| `refactor` | 重构 | `refactor: Refactor animation controller` |
| `test` | 测试相关 | `test: Add unit tests` |
| `chore` | 构建/工具相关 | `chore: Update dependencies` |
| `merge` | 合并分支 | `merge: Sync with upstream` |

---

## 常见问题

### 推送被拒绝

```bash
# 如果远程有新的提交，先拉取合并
git pull fork master
# 然后再推送
git push fork master
```

### 忘记切换分支就开始开发了

```bash
# 暂存当前修改
git stash

# 创建并切换到新分支
git checkout -b feature/new-feature

# 恢复修改
git stash pop
```

### 提交了不应该提交的文件

```bash
# 从 Git 中移除（保留本地文件）
git rm --cached <文件>

# 添加到 .gitignore
echo "<文件>" >> .gitignore

# 提交
git commit -m "chore: Remove unwanted file"
```

---

## 快捷别名配置（可选）

在 `~/.gitconfig` 中添加：

```ini
[alias]
    st = status
    co = checkout
    br = branch
    ci = commit
    lg = log --oneline -10
    last = log -1 HEAD
    unstage = reset HEAD --
    pushf = push fork master
    sync = !git fetch origin && git merge origin/master
```

配置后可以使用：

```bash
git st          # = git status
git co master   # = git checkout master
git lg          # = git log --oneline -10
git pushf       # = git push fork master
git sync        # = 从官方同步
```

