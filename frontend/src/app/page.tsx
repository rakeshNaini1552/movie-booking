import MovieList from "@/components/MovieList";
import styles from "./page.module.css";
import Header from "@/components/Header";

export default function Home() {
  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <div className={styles.intro}>
          <Header title="Movie Booking" />
          <MovieList />
        </div>
      </main>
    </div>
  );
}
