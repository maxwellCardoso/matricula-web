import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';

import { AlertComponent } from './shared/components/alert/alert';

interface NavItem {
  label: string;
  path: string;
  isActive: (url: string) => boolean;
}

@Component({
  selector: 'app-root',
  imports: [AlertComponent, RouterLink, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);

  protected readonly navItems: NavItem[] = [
    {
      label: 'Alunos',
      path: '/alunos',
      isActive: (url) => url.startsWith('/alunos'),
    },
    {
      label: 'Cursos',
      path: '/cursos',
      isActive: (url) => url.startsWith('/cursos'),
    },
    {
      label: 'Disciplinas',
      path: '/disciplinas',
      isActive: (url) => url.startsWith('/disciplinas'),
    },
    {
      label: 'Turmas',
      path: '/turmas',
      isActive: (url) => url.startsWith('/turmas'),
    },
    {
      label: 'Matrículas',
      path: '/matriculas',
      isActive: (url) => url === '/matriculas' || url.startsWith('/matriculas/'),
    },
  ];

  protected isNavActive(item: NavItem): boolean {
    const url = this.router.url.split('?')[0];
    return item.isActive(url);
  }
}
