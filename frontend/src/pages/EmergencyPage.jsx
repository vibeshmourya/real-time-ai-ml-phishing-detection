import React from 'react';
import { EmergencyWizardContent } from '../components/EmergencyWizard';

export default function EmergencyPage() {
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <EmergencyWizardContent standalone={true} />
    </div>
  );
}
