import type { Metadata } from 'next';
import { InvitationApplicationFlow } from '@/components/invitation-application';
import { SITE_NAME } from '@/lib/site';

export const metadata: Metadata = {
  title: `Convite de onboarding · ${SITE_NAME}`,
  robots: {
    index: false,
    follow: false,
  },
};

export default async function InvitationPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  return <InvitationApplicationFlow token={token} />;
}
