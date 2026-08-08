import type { Metadata } from 'next';
import { AdminDashboard } from '@/components/admin-dashboard';
import { SITE_NAME } from '@/lib/site';

export const metadata: Metadata = {
  title: `Admin · ${SITE_NAME}`,
  description: 'Área restrita para acompanhamento interno de onboarding.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function AdminPage() {
  return <AdminDashboard />;
}
