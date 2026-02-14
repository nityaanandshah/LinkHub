import { ReactNode } from 'react';
import Navbar from './Navbar';

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen" style={{ backgroundColor: '#F8FAFC' }}>
      <Navbar />
      <main className="max-w-7xl mx-auto px-6 sm:px-8 lg:px-10 py-10">
        {children}
      </main>
    </div>
  );
}
