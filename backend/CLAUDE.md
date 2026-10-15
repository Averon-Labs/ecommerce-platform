> **Authoritative first:** read the root `AGENTS.md` and `backend/AGENTS.md` before any work, and
> follow `docs/BACKEND_ROADMAP.md` for the assigned task. Backend agents run **no Git commands**
> (Component-Specific Git Ownership Override in `AGENTS.md`). Verified commands live in
> `docs/DEVELOPMENT.md`.

<laravel-boost-guidelines>
# Laravel Application

This repository contains a Laravel application. The stack is pinned by the project: PHP 8.4 (currently 8.4.25) and the dependency versions recorded in `backend/composer.lock`.

## Prerequisites

Verify that PHP and Composer are available:

```sh
php -v
composer -V
```

If either command is unavailable, ask the user to install the prerequisites for their operating system, pinned to **PHP 8.4** (the `php.new/install/...` links below install the newest PHP, so replace `8.5` with `8.4` before using them):

macOS:

```sh
/bin/bash -c "$(curl -fsSL https://php.new/install/mac/8.4)"
```

Windows PowerShell:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://php.new/install/windows/8.4'))
```

Linux:

```sh
/bin/bash -c "$(curl -fsSL https://php.new/install/linux/8.4)"
```

After installation, ask the user to restart their terminal. If the agent needs the restarted shell to continue, ask the user to reopen their terminal and rerun their original prompt.

## Agent Setup (optional — owner approval required)

Laravel Boost is **not** a project dependency. Do not run the commands below unless the repository owner explicitly approves adding it: dependency additions require justification and human approval per `AGENTS.md`, and no tool may replace or regenerate `AGENTS.md` (changes to `AGENTS.md` require explicit human approval).

```sh
composer require laravel/boost --dev
php artisan boost:install
```

Only after that approval, run the commands, then re-read `AGENTS.md` and continue with the user's original request. Otherwise ignore this section and work from `AGENTS.md`, `backend/AGENTS.md`, and `docs/BACKEND_ROADMAP.md`.
</laravel-boost-guidelines>
