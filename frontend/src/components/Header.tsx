type HeaderProps = {
  title: string;
  // Optional small line above the title, for example "Now showing".
  eyebrow?: string;
};

export default function Header({ title, eyebrow }: HeaderProps) {
  return (
    <div className="mb-8">
      {eyebrow && (
        <p className="text-brand-soft mb-2 text-xs font-semibold tracking-[0.2em] uppercase">
          {eyebrow}
        </p>
      )}
      <h1 className="font-display text-4xl font-bold tracking-tight text-balance sm:text-5xl">
        {title}
      </h1>
    </div>
  );
}
