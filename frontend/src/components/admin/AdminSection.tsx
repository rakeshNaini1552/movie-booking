import type { ReactNode } from "react";

type AdminSectionProps = {
  id: string;
  title: string;
  children: ReactNode;
  // Optional control on the right of the title, for example an "Add" link.
  action?: ReactNode;
};

export default function AdminSection({ id, title, children, action }: AdminSectionProps) {
  return (
    <section aria-labelledby={id} className="border-line bg-panel rounded-3xl border p-6">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h2 id={id} className="font-display text-xl font-bold">
          {title}
        </h2>
        {action}
      </div>
      {children}
    </section>
  );
}
