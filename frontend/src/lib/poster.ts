// The catalog has no poster images, so each movie gets a generated poster. The colour comes from
// the title, which means the same movie always gets the same poster on every page.
export function posterHue(title: string): number {
  let hash = 0;
  for (const char of title) hash = (hash * 31 + char.codePointAt(0)!) % 360;
  return hash;
}

export function posterBackground(title: string): string {
  const hue = posterHue(title);
  const second = (hue + 48) % 360;
  return [
    `radial-gradient(120% 80% at 80% 0%, hsl(${second} 85% 55% / 0.55), transparent 60%)`,
    `linear-gradient(165deg, hsl(${hue} 70% 32%), hsl(${hue} 60% 9%) 78%)`,
  ].join(", ");
}
