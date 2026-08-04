'use client';

import './globals.css';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  const links = [
    { href: '/', label: 'Home', icon: '' },
    { href: '/book', label: 'Book', icon: '' },
    { href: '/admin', label: 'Admin', icon: '' },
  ];

  return (
    <html lang="en">
      <head>
        <title>RailSeat LK: Segment-Based Train Booking</title>
        <meta name="description" content="Book reserved seats on Sri Lanka's scenic Colombo Fort to Badulla train line. Segment-based booking lets you pay only for the distance you travel." />
      </head>
      <body>
        <nav className="nav">
          <div className="nav-inner">
            <Link href="/" className="nav-logo">
              <span className="nav-logo-icon"></span>
              <span>RailSeat LK</span>
            </Link>
            <div className="nav-links">
              {links.filter(link => !(link.href === '/book' && ['/', '/admin'].includes(pathname))).map(link => (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`nav-link ${pathname === link.href ? 'active' : ''}`}
                >
                  {link.icon} {link.label}
                </Link>
              ))}
            </div>
          </div>
        </nav>
        <main className="main page-enter">
          {children}
        </main>
      </body>
    </html>
  );
}
