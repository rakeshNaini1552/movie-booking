import Link from "next/link";

export default function NotFound() {
  return (
    <main>
      <p>Page not found.</p>
      <Link href="/">Return home</Link>
    </main>
  );
}
