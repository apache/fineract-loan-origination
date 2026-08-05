/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import { Injectable, effect, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'los-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(this.readInitialTheme());

  constructor() {
    // Applies the theme to <html data-theme="..."> and persists it, any time it changes.
    effect(() => {
      const theme = this.theme();
      document.documentElement.setAttribute('data-theme', theme);
      try {
        localStorage.setItem(STORAGE_KEY, theme);
      } catch {
        // localStorage unavailable (private browsing, storage disabled) — theme just won't persist.
      }
    });
  }

  toggle(): void {
    this.theme.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  private readInitialTheme(): Theme {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'dark' || stored === 'light') return stored;
    } catch {
      // ignore — fall through to system preference
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
