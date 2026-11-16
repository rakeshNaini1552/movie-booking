type HeaderProps = {
  title: string;
};

export default function Header({ title }: HeaderProps) {
  return <h1 className="mb-6 text-3xl font-bold tracking-tight">{title}</h1>;
}
