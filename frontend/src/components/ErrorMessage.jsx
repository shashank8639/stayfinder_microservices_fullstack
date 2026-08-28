export default function ErrorMessage({ message }) {
  if (!message) {
    return null;
  }
  return <p className="error-banner">{message}</p>;
}
